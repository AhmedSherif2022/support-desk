package com.supportdesk.azure.service;

import com.supportdesk.azure.config.AzureDevOpsProperties;
import com.supportdesk.azure.dto.EscalationPayload;
import com.supportdesk.azure.entity.Escalation;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.entity.CustomerActivity;
import com.supportdesk.customer.enums.ActivityResult;
import com.supportdesk.customer.repository.CustomerActivityRepository;
import com.supportdesk.guide.entity.GuideRun;
import com.supportdesk.guide.repository.GuideRunRepository;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import com.supportdesk.monitor.repository.DetectedErrorEventRepository;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.service.SlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.supportdesk.azure.service.PersonalDataScrubber.scrub;

/** E5-S1: collects what SRE needs for a ticket's work item, without any personal data. */
@Component
@RequiredArgsConstructor
public class EscalationPayloadBuilder
{
    static final Duration LOG_WINDOW = Duration.ofHours(24);
    static final int LOG_LINES = 5;
    static final int SUMMARY_MAX = 1500;

    private final AzureDevOpsProperties props;
    private final CustomerTokenService tokens;
    private final CustomerActivityRepository activityRepository;
    private final DetectedErrorEventRepository errorEventRepository;
    private final GuideRunRepository guideRunRepository;
    private final SlaService slaService;

    public EscalationPayload build(Ticket ticket, Escalation escalation, String idempotencyKey)
    {
        Customer customer = ticket.getCustomer();
        String name = customer.getFullName();
        TicketCategory category = ticket.getCategory();
        String service = category.getParent() == null ? category.getCode() : category.getParent().getCode();

        String summary = scrub(ticket.getTitle() + " - " + ticket.getDescription(), name);
        if (summary.length() > SUMMARY_MAX) {
            summary = summary.substring(0, SUMMARY_MAX) + "...";
        }

        return new EscalationPayload(
                idempotencyKey,
                ticket.displayNumber(),
                trimSlash(props.portalBaseUrl()) + "/tickets/" + ticket.getTicketNumber(),
                tokens.tokenFor(customer.getId()),
                ticket.getPlatform() == null ? "-" : ticket.getPlatform().name(),
                service,
                category.fullName(),
                ticket.customerErrorCode(),
                summary,
                logSnapshot(customer.getId(), escalation.getEscalatedAt()),
                correlationId(ticket),
                ticket.getPriority().name(),
                ticket.getSlaDueAt(),
                slaService.evaluate(ticket).label(),
                guidePath(ticket),
                scrub(escalation.getReason(), name),
                escalation.getUrgency());
    }

    /** The customer's latest failures before the escalation: time, platform, service, event, error code. */
    private List<String> logSnapshot(Long customerId, Instant until)
    {
        List<CustomerActivity> recent = activityRepository
                .findByCustomerIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
                        customerId, until.minus(LOG_WINDOW), until.plusSeconds(1));
        return recent.stream()
                .filter(a -> a.getResult() == ActivityResult.FAILURE)
                .limit(LOG_LINES)
                .map(a -> a.getOccurredAt() + " " + a.getPlatform() + " " + a.getService() + " " + a.getEventType()
                        + (a.getErrorCode() == null ? "" : " " + a.getErrorCode()))
                .toList();
    }

    /** The monitor's correlation ID when the ticket came from a detected error (E6), else none. */
    private String correlationId(Ticket ticket)
    {
        return errorEventRepository.findByTicketIdOrderByOccurredAtDesc(ticket.getId()).stream()
                .map(DetectedErrorEvent::getCorrelationId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** The latest guide run: guide title, the steps answered, and the outcome. */
    private String guidePath(Ticket ticket)
    {
        List<GuideRun> runs = guideRunRepository.findByTicketIdOrderByStartedAtDescIdDesc(ticket.getId());
        if (runs.isEmpty()) {
            return null;
        }
        GuideRun run = runs.get(0);
        String steps = run.getAnswers().stream()
                .map(a -> a.getStepKey() + "=" + a.getAnswerKey())
                .collect(Collectors.joining(" > "));
        String outcome = run.getOutcomeType() == null ? "not finished" : run.getOutcomeType().name();
        return run.getPinnedVersion().getGuide().getTitle() + ": " + (steps.isEmpty() ? "no answers" : steps)
                + " -> " + outcome;
    }

    private static String trimSlash(String url)
    {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
