package com.supportdesk.ticket.service;

import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.customer.enums.ActivityResult;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.entity.CustomerActivity;
import com.supportdesk.customer.repository.CustomerActivityRepository;
import com.supportdesk.customer.service.CallerVerificationService;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.customer.service.CustomerLookupService;
import com.supportdesk.guide.entity.GuideRun;
import com.supportdesk.guide.repository.GuideRunRepository;
import com.supportdesk.guide.service.GuideMatcher;
import com.supportdesk.ticket.dto.TicketDtos.CategoryDto;
import com.supportdesk.ticket.dto.TicketDtos.CreateTicketRequest;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckDto;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckRequest;
import com.supportdesk.ticket.dto.TicketDtos.TakeStepRequest;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import com.supportdesk.ticket.dto.TicketMapper;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.StepActor;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.enums.TicketStep;
import com.supportdesk.ticket.event.TicketCreatedEvent;
import com.supportdesk.ticket.event.TicketStatusChangedEvent;
import com.supportdesk.ticket.event.TicketTransitionRejectedEvent;
import com.supportdesk.ticket.exception.CategoryNotFoundException;
import com.supportdesk.ticket.exception.DuplicateTicketException;
import com.supportdesk.ticket.exception.InvalidStatusTransitionException;
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
    private final TicketStepMachine stepMachine;
    private final CustomerLookupService customerLookup;
    private final CallerVerificationService verificationService;
    private final CurrentUserService currentUserService;
    private final SlaService slaService;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final CustomerActivityRepository activityRepository;
    private final GuideMatcher guideMatcher;
    private final GuideRunRepository guideRuns;




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


    @Transactional(readOnly = true)
    public List<CategoryDto> categories()
    {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> new CategoryDto(c.getCode(), c.getName(), c.getParent() == null ? null : c.getParent().getCode(),
                        c.getDefaultPriority(), c.isReferenceRequired(), c.getReferenceHint()))
                .toList();
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




    /** POST /api/tickets/{number}/steps (agent) and /api/review/tickets/{number}/steps (reviewer). */
    @Transactional
    public TicketDto takeStep(Long ticketNumber, TakeStepRequest request)
    {
        Ticket ticket = getEntity(ticketNumber);
        applyStep(ticket, request.step(), currentActor(), currentUserService.getCurrentUserEntity(), request.reason());
        return toDto(ticket);
    }

    /** The only way a ticket changes status: the step machine says who and from where, the status machine guards the move. */
    @Transactional
    public void applyStep(Ticket ticket, TicketStep step, StepActor actor, User user, String reason)
    {
        stepMachine.checkActor(step, actor);                                   // 422 STEP_NOT_YOURS
        if (step == TicketStep.REVIEWER_REJECTS && isBlank(reason)) {
            throw new BusinessRuleException(ErrorCode.REASON_REQUIRED, "Say why the fix is rejected.");
        }

        TicketStatus from = ticket.getStatus();
        TicketStatus to = step.getTo();
        Long userId = user == null ? null : user.getId();
        if (step.getFrom() != from || !statusMachine.canMove(from, to)) {
            // audit TICKET_TRANSITION_REJECTED, then 422
            events.publishEvent(new TicketTransitionRejectedEvent(ticket.getId(), ticket.getTicketNumber(),
                    from, to, userId));
            throw new InvalidStatusTransitionException(step, from);
        }

        Instant now = clock.instant();
        ticket.moveTo(to, now);
        ticket.updateSlaState(slaService.evaluate(ticket, now).state());

        events.publishEvent(new TicketStatusChangedEvent(ticket.getId(), ticket.getTicketNumber(),
                from, to, step, isBlank(reason) ? null : reason.trim(), userId));
    }

    /** The Reviewer's queue: SRE fixes waiting for review, oldest first. */
    @Transactional(readOnly = true)
    public List<TicketDto> inReview()
    {
        return ticketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.IN_REVIEW).stream().map(this::toDto).toList();
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
        // Outside Troubleshooting an unfinished run is stale: starting a guide abandons it.
        Long activeRun = ticket.getStatus() != TicketStatus.TROUBLESHOOTING ? null
                : guideRuns.findByTicketIdAndCompletedAtIsNull(ticket.getId()).map(GuideRun::getId).orElse(null);
        return TicketMapper.toDto(ticket, stepMachine.available(ticket.getStatus(), currentActor()), slaService.evaluate(ticket),
                guideMatcher.hasGuideFor(ticket), activeRun);
    }

    private StepActor currentActor()
    {
        return StepActor.of(currentUserService.getCurrentUser().role());
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
