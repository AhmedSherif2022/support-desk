package com.supportdesk.customer.service;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.customer.dto.Customer360Dtos.ActivityCounts;
import com.supportdesk.customer.dto.Customer360Dtos.ActivityEventDto;
import com.supportdesk.customer.dto.Customer360Dtos.ActivityResponse;
import com.supportdesk.customer.dto.Customer360Dtos.CustomerDetailDto;
import com.supportdesk.customer.dto.Customer360Dtos.CustomerTicketRow;
import com.supportdesk.customer.dto.Customer360Dtos.CustomerTicketsDto;
import com.supportdesk.customer.dto.Customer360Dtos.DiagnosisDto;
import com.supportdesk.customer.dto.Customer360Dtos.FailureSummary;
import com.supportdesk.customer.dto.Customer360Dtos.RevealedFieldDto;
import com.supportdesk.customer.enums.ActivityRange;
import com.supportdesk.customer.entity.CallerVerification;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.entity.CustomerActivity;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.customer.enums.SensitiveField;
import com.supportdesk.customer.repository.CustomerActivityRepository;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.repository.TicketCategoryRepository;
import com.supportdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Customer 360 (E2-S2..S6). Every method needs a verified caller for the current agent; without one it answers
 * 403 CALLER_NOT_VERIFIED and shows nothing about the customer.
 */
@Service
@RequiredArgsConstructor
public class Customer360Service
{
    static final Duration DIAGNOSIS_WINDOW = Duration.ofHours(6);
    static final Duration REPEAT_CALLER_WINDOW = Duration.ofDays(20);
    static final String NO_FAILURES = "No failures in the last 6 hours. Check the customer's device or network.";
    static final String NO_EVENTS =
            "No events match. Widen the time range, include successes, or clear the platform and service chips.";
    // "Today" is the bank's day, not the server's: the bank and its customers are in Egypt.
    static final ZoneId BANK_ZONE = ZoneId.of("Africa/Cairo");

    private final CustomerLookupService customerLookup;
    private final CallerVerificationService verificationService;
    private final CustomerActivityRepository activityRepository;
    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional   // writes CUSTOMER_DATA_VIEWED in the same transaction (E1-S4)
    public CustomerDetailDto detail(String cif)
    {
        Customer c = customerLookup.getByCif(cif);
        CallerVerification verification = verificationService.requireVerified(c);
        auditService.recordForCurrentUser(AuditAction.CUSTOMER_DATA_VIEWED, "CUSTOMER", c.getCif(), null, null);
        return new CustomerDetailDto(c.getCif(), c.getFullName(), c.getSegment(), Masking.nationalId(c.getNationalId()),
                Masking.mobile(c.getMobile()), c.getEnrollmentStatus(), c.getPreferredLanguage(),
                verification.getVerifiedAt());
    }

    @Transactional
    public RevealedFieldDto reveal(String cif, SensitiveField field)
    {
        Customer c = customerLookup.getByCif(cif);
        verificationService.requireVerified(c);
        // The audit names the field, never its value.
        auditService.recordForCurrentUser(AuditAction.CUSTOMER_FIELD_REVEALED, "CUSTOMER", c.getCif(), null,
                Map.of("field", field));
        return new RevealedFieldDto(field, switch (field) {
            case NATIONAL_ID -> c.getNationalId();
            case MOBILE -> c.getMobile();
        });
    }

    @Transactional(readOnly = true)
    public DiagnosisDto diagnosis(String cif)
    {
        Customer c = customerLookup.getByCif(cif);
        verificationService.requireVerified(c);
        Instant now = clock.instant();
        List<CustomerActivity> failures = activity(c, now.minus(DIAGNOSIS_WINDOW), now).stream()
                .filter(CustomerActivity::isFailure)
                .toList();

        List<String> failingServices = failures.stream().map(CustomerActivity::getService).distinct().toList();
        List<String> servicesWithoutFailures = services().stream().filter(s -> !failingServices.contains(s)).toList();
        if (failures.isEmpty()) {
            return new DiagnosisDto(hours(DIAGNOSIS_WINDOW), false, null, NO_FAILURES, servicesWithoutFailures);
        }

        // Most frequent (event type, error code, service); a tie goes to the most recent.
        FailureSummary top = failures.stream()
                .collect(Collectors.groupingBy(a -> List.of(a.getEventType(), String.valueOf(a.getErrorCode()), a.getService())))
                .values().stream()
                .map(group -> new FailureSummary(group.size(), group.get(0).getEventType(), group.get(0).getErrorCode(),
                        group.get(0).getService(), group.get(group.size() - 1).getOccurredAt(), group.get(0).getOccurredAt()))
                .max(Comparator.comparingLong(FailureSummary::count).thenComparing(FailureSummary::lastAt))
                .orElseThrow();
        // TODO (E6 incidents / E4 guides): name a matching open incident and offer "Run guide" once those exist.
        String summary = "%d failed %s %s with %s on %s in the last %d hours.".formatted(top.count(), top.eventType(),
                top.count() == 1 ? "event" : "events", top.errorCode() == null ? "no error code" : top.errorCode(),
                top.service(), hours(DIAGNOSIS_WINDOW));
        return new DiagnosisDto(hours(DIAGNOSIS_WINDOW), true, top, summary, servicesWithoutFailures);
    }

    @Transactional(readOnly = true)
    public ActivityResponse activity(String cif, ActivityRange range, Instant customFrom, Instant customTo,
                                     Platform platform, String service, boolean failuresOnly)
    {
        Customer c = customerLookup.getByCif(cif);
        verificationService.requireVerified(c);
        ActivityRange selected = range == null ? ActivityRange.LAST_6_HOURS : range;
        Instant now = clock.instant();
        Instant from = switch (selected) {
            case LAST_6_HOURS -> now.minus(Duration.ofHours(6));
            case TODAY -> LocalDate.now(clock.withZone(BANK_ZONE)).atStartOfDay(BANK_ZONE).toInstant();
            case LAST_7_DAYS -> now.minus(Duration.ofDays(7));
            case CUSTOM -> customFrom;
        };
        Instant to = selected == ActivityRange.CUSTOM ? customTo : now;
        if (from == null || to == null || !from.isBefore(to)) {
            throw new BusinessRuleException(ErrorCode.CUSTOM_RANGE_REQUIRED);
        }

        List<CustomerActivity> inRange = activity(c, from, to).stream()
                .filter(a -> !failuresOnly || a.isFailure())
                .toList();
        // Chip counts are taken before the chips filter, so every chip shows how many it would keep.
        ActivityCounts counts = new ActivityCounts(inRange.size(),
                inRange.stream().filter(CustomerActivity::isFailure).count(),
                inRange.stream().collect(Collectors.groupingBy(CustomerActivity::getPlatform,
                        () -> new EnumMap<>(Platform.class), Collectors.counting())),
                inRange.stream().collect(Collectors.groupingBy(CustomerActivity::getService, TreeMap::new,
                        Collectors.counting())));
        List<ActivityEventDto> events = inRange.stream()
                .filter(a -> platform == null || a.getPlatform() == platform)
                .filter(a -> service == null || service.isBlank() || a.getService().equalsIgnoreCase(service.trim()))
                .map(a -> new ActivityEventDto(a.getOccurredAt(), a.getEventType(), a.getPlatform(), a.getService(),
                        a.getResult(), a.getErrorCode(), a.isFailure()))
                .toList();
        return new ActivityResponse(selected, from, to, failuresOnly, counts, events, events.isEmpty() ? NO_EVENTS : null);
    }

    @Transactional(readOnly = true)
    public CustomerTicketsDto tickets(String cif)
    {
        Customer c = customerLookup.getByCif(cif);
        verificationService.requireVerified(c);
        List<Ticket> tickets = ticketRepository.findByCustomerIdOrderByCreatedAtDesc(c.getId());
        Instant since = clock.instant().minus(REPEAT_CALLER_WINDOW);
        long recent = tickets.stream().filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since)).count();
        boolean repeatCaller = recent > 1;
        // TODO (E5 escalation): add the Azure DevOps work item number and the Incidents & RCA tab.
        return new CustomerTicketsDto(repeatCaller,
                repeatCaller ? "Repeat caller: %d tickets in the last 20 days.".formatted(recent) : null,
                tickets.stream().map(t -> new CustomerTicketRow(t.displayNumber(), t.getTitle(), t.getCategory().fullName(),
                        t.getOrigin(), t.getStatus(), t.getPriority(), t.getSlaState(), t.getCreatedAt())).toList());
    }

    private List<CustomerActivity> activity(Customer c, Instant from, Instant to)
    {
        return activityRepository.findByCustomerIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
                c.getId(), from, to);
    }

    /** The service catalogue is the active top-level ticket categories, so diagnosis and tickets use one vocabulary. */
    private List<String> services()
    {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(cat -> !cat.isSubCategory())
                .map(TicketCategory::getCode)
                .sorted()
                .toList();
    }

    private static int hours(Duration d)
    {
        return (int) d.toHours();
    }
}
