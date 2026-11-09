package com.supportdesk.ticket.service;

import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.customer.enums.ActivityResult;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.entity.CustomerActivity;
import com.supportdesk.customer.repository.CustomerActivityRepository;
import com.supportdesk.customer.service.CallerVerificationService;
import com.supportdesk.customer.service.CustomerLookupService;
import com.supportdesk.guide.service.GuideMatcher;
import com.supportdesk.ticket.dto.TicketDtos.ChangeStatusRequest;
import com.supportdesk.ticket.dto.TicketDtos.CreateTicketRequest;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckDto;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckRequest;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import com.supportdesk.ticket.dto.TicketMapper;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.event.TicketCreatedEvent;
import com.supportdesk.ticket.event.TicketStatusChangedEvent;
import com.supportdesk.ticket.event.TicketTransitionRejectedEvent;
import com.supportdesk.ticket.exception.CategoryNotFoundException;
import com.supportdesk.ticket.exception.DuplicateTicketException;
import com.supportdesk.ticket.exception.InvalidStatusTransitionException;
import com.supportdesk.ticket.exception.StatusSetBySystemException;
import com.supportdesk.ticket.exception.TicketNotFoundException;
import com.supportdesk.ticket.repository.TicketCategoryRepository;
import com.supportdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class TicketService
{

    private static final List<TicketStatus> CLOSED_STATUSES = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;
    private final TicketValidator validator;
    private final TicketStatusMachine statusMachine;
    private final CustomerLookupService customerLookup;
    private final CallerVerificationService verificationService;
    private final CurrentUserService currentUserService;
    private final SlaService slaService;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final CustomerActivityRepository activityRepository;
    private final GuideMatcher guideMatcher;




    @Transactional
    public TicketDto create(CreateTicketRequest request)
    {
        Customer customer = customerLookup.getByCif(request.cif());
        verificationService.requireVerified(customer);

        TicketCategory category = findCategory(request.categoryCode());
        validator.checkCategory(category);
        String reference = validator.checkReference(category, request.reference());

        if (!request.allowDuplicate()) {
            findOpenDuplicate(customer, category).ifPresent(existing -> {
                throw new DuplicateTicketException(existing.getTicketNumber());
            });
        }


        TicketPriority priority = request.priority() != null ? request.priority() : category.getDefaultPriority();
        String title = isBlank(request.title()) ? category.getName() : request.title().trim();
        Optional<String> customerErrorCode = isBlank(request.errorCode()) ? latestFailureCode(customer, category)
                : Optional.of(request.errorCode().trim().toUpperCase(Locale.ROOT));
        String errorCode = customerErrorCode.orElse(category.getDefaultErrorCode());


        Instant now = clock.instant();
        User agent = currentUserService.getCurrentUserEntity();

        Ticket ticket = new Ticket(ticketRepository.nextTicketNumber(), customer, category, title,
                request.description().trim(), priority, TicketOrigin.MANUAL, request.platform(),
                errorCode, reference, agent, now, slaService.dueAt(priority, now));
        ticket.assignTo(agent);                                              // the creator owns it
        if (customerErrorCode.isEmpty()) {
            ticket.markErrorCodeAsCategoryDefault();
        }
        ticketRepository.save(ticket);


        events.publishEvent(new TicketCreatedEvent(ticket.getId(), ticket.getTicketNumber(),
                customer.getId(), agent.getId()));
        return toDto(ticket);
    }


    @Transactional(readOnly = true)   // Spring: read-only transaction
    public PrecheckDto precheck(PrecheckRequest request)
    {
        Customer customer = customerLookup.getByCif(request.cif());
        verificationService.requireVerified(customer);
        TicketCategory category = findCategory(request.categoryCode());

        List<PrecheckDto.Warning> warnings = new ArrayList<>();
        findOpenDuplicate(customer, category).ifPresent(existing -> warnings.add(new PrecheckDto.Warning(
                "DUPLICATE_OPEN_TICKET",
                "Ticket " + existing.displayNumber() + " is already open for this problem.",
                existing.displayNumber())));
        // TODO (E6 incidents): open incident on the same service → warning "Will link to INC-301".
        return new PrecheckDto(warnings);
    }




    @Transactional
    public TicketDto changeStatus(Long ticketNumber, ChangeStatusRequest request)
    {
        Ticket ticket = getEntity(ticketNumber);
        if (!statusMachine.isAgentChoice(request.status())) {
            throw new StatusSetBySystemException(request.status());          // 422
        }
        moveStatus(ticket, request.status(), currentUserService.getCurrentUserEntity());
        return toDto(ticket);
    }


    @Transactional
    public void moveStatus(Ticket ticket, TicketStatus to, User actor)
    {
        TicketStatus from = ticket.getStatus();
        Long actorId = actor == null ? null : actor.getId();

        if (!statusMachine.canMove(from, to)) {
            // audit TICKET_TRANSITION_REJECTED, then 422
            events.publishEvent(new TicketTransitionRejectedEvent(ticket.getId(), ticket.getTicketNumber(),
                    from, to, actorId));
            throw new InvalidStatusTransitionException(from, to);
        }

        Instant now = clock.instant();
        ticket.moveTo(to, now);
        ticket.updateSlaState(slaService.evaluate(ticket, now).state());

        events.publishEvent(new TicketStatusChangedEvent(ticket.getId(), ticket.getTicketNumber(),
                from, to, actorId));
    }



    @Transactional(readOnly = true)
    public TicketDto get(Long ticketNumber)
    {
        return toDto(getEntity(ticketNumber));
    }


    @Transactional(readOnly = true)
    public Ticket getEntity(Long ticketNumber)
    {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new TicketNotFoundException(ticketNumber));
    }



    private TicketCategory findCategory(String code)
    {
        return categoryRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new CategoryNotFoundException(code));
    }


    private Optional<Ticket> findOpenDuplicate(Customer customer, TicketCategory category)
    {
        return ticketRepository.findFirstByCustomerIdAndCategoryIdAndStatusNotIn(
                customer.getId(), category.getId(), CLOSED_STATUSES);
    }

    private TicketDto toDto(Ticket ticket)
    {
        return TicketMapper.toDto(ticket, statusMachine.agentChoicesFrom(ticket.getStatus()), slaService.evaluate(ticket),
                guideMatcher.hasGuideFor(ticket));
    }

    /** E3-S1: the error code of the customer's latest failure on this ticket's service. */
    private Optional<String> latestFailureCode(Customer customer, TicketCategory category)
    {
        String service = category.getParent() == null ? category.getCode() : category.getParent().getCode();
        return activityRepository.findFirstByCustomerIdAndServiceAndResultOrderByOccurredAtDesc(
                        customer.getId(), service, ActivityResult.FAILURE)
                .map(CustomerActivity::getErrorCode);
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }
}
