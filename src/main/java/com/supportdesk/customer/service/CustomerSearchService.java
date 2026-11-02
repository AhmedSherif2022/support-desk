package com.supportdesk.customer.service;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.customer.dto.Customer360Dtos.CustomerSearchResponse;
import com.supportdesk.customer.dto.Customer360Dtos.CustomerSearchResult;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.repository.CustomerRepository;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * E2-S1: one search box for CIF, national ID, mobile or ticket number. Results are masked; nothing is shown
 * beyond the search card until the caller is verified.
 */
@Service
@RequiredArgsConstructor
public class CustomerSearchService
{
    static final String NO_MATCH = "No match. Check the digits and the ID type.";
    private static final List<TicketStatus> CLOSED_STATUSES = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final CustomerRepository customerRepository;
    private final TicketRepository ticketRepository;
    private final SearchRateLimiter rateLimiter;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Transactional   // writes CUSTOMER_SEARCHED in the same transaction
    public CustomerSearchResponse search(String query)
    {
        rateLimiter.check(currentUserService.getCurrentUserId());
        String q = query == null ? "" : query.trim();

        Map<Long, CustomerSearchResult> found = new LinkedHashMap<>();   // by customer id: one row per customer
        customerRepository.findByCifIgnoreCase(q).ifPresent(c -> add(found, c, "CIF"));
        customerRepository.findByNationalId(q).ifPresent(c -> add(found, c, "NATIONAL_ID"));
        mobile(q).ifPresent(m -> customerRepository.findByMobile(m).forEach(c -> add(found, c, "MOBILE")));
        ticketNumber(q).flatMap(ticketRepository::findByTicketNumber)
                .map(Ticket::getCustomer)
                .ifPresent(c -> add(found, c, "TICKET"));
        // TODO (E5 escalation): match Azure DevOps work item numbers once tickets store them.

        List<CustomerSearchResult> results = List.copyOf(found.values());
        // E1-S4: never store the raw search text, only what kind of value matched and how many results.
        auditService.recordForCurrentUser(AuditAction.CUSTOMER_SEARCHED, "CUSTOMER_SEARCH", null, null, Map.of(
                "matchedBy", results.stream().map(CustomerSearchResult::matchedBy).distinct().toList(),
                "resultCount", results.size()));
        return new CustomerSearchResponse(results, results.isEmpty() ? NO_MATCH : null);
    }

    private void add(Map<Long, CustomerSearchResult> found, Customer c, String matchedBy)
    {
        found.computeIfAbsent(c.getId(), id -> new CustomerSearchResult(c.getCif(), c.getFullName(), c.getSegment(),
                Masking.mobile(c.getMobile()), c.getEnrollmentStatus(),
                ticketRepository.countByCustomerIdAndStatusNotIn(c.getId(), CLOSED_STATUSES), matchedBy));
    }

    /** "01001234567", "201001234567" and "+201001234567" are the same Egyptian mobile. */
    static Optional<String> mobile(String q)
    {
        String digits = q.replaceAll("[\\s-]", "");
        if (digits.matches("\\+\\d{10,14}")) {
            return Optional.of(digits);
        }
        if (digits.matches("0\\d{10}")) {
            return Optional.of("+20" + digits.substring(1));
        }
        if (digits.matches("20\\d{10}")) {
            return Optional.of("+" + digits);
        }
        return Optional.empty();
    }

    /** "#2001" or "2001"; national IDs (14 digits) and mobiles are too long to be ticket numbers. */
    static Optional<Long> ticketNumber(String q)
    {
        String digits = q.startsWith("#") ? q.substring(1) : q;
        return digits.matches("\\d{1,9}") ? Optional.of(Long.parseLong(digits)) : Optional.empty();
    }
}
