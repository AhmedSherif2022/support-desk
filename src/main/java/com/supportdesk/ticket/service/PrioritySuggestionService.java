package com.supportdesk.ticket.service;

import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.ticket.dto.PrioritySuggestionDto;
import com.supportdesk.ticket.dto.PrioritySuggestionDto.Basis;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.exception.CategoryNotFoundException;
import com.supportdesk.ticket.repository.TicketCategoryRepository;
import com.supportdesk.ticket.repository.TicketRepository;
import com.supportdesk.ticket.repository.TicketRepository.PriorityCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Suggests the priority for a new ticket from its sub-category. Starts from the sub-category's default priority;
 * when agents clearly chose another priority for recent tickets in the same sub-category (at least
 * {@value #MIN_TICKETS} manual tickets in the last 90 days, and more than half of them agree), suggests that instead.
 * The agent always makes the final choice.
 */
@Service
@RequiredArgsConstructor
public class PrioritySuggestionService
{
    static final int MIN_TICKETS = 3;
    static final Duration HISTORY_WINDOW = Duration.ofDays(90);

    private final TicketCategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;
    private final SlaService slaService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PrioritySuggestionDto suggest(String categoryCode)
    {
        TicketCategory category = categoryRepository.findByCodeIgnoreCase(categoryCode == null ? "" : categoryCode.trim())
                .orElseThrow(() -> new CategoryNotFoundException(categoryCode));
        if (!category.isSubCategory()) {
            throw new BusinessRuleException(ErrorCode.SUBCATEGORY_REQUIRED,
                    category.getCode() + " is a category; suggest a priority for one of its sub-categories.");
        }

        Map<TicketPriority, Long> recent = new EnumMap<>(TicketPriority.class);
        for (PriorityCount row : ticketRepository.countManualPrioritiesSince(category.getId(),
                clock.instant().minus(HISTORY_WINDOW))) {
            recent.put(row.getPriority(), row.getTickets());
        }
        long total = recent.values().stream().mapToLong(Long::longValue).sum();
        TicketPriority fallback = category.getDefaultPriority();

        TicketPriority suggested = fallback;
        Basis basis = Basis.CATEGORY_DEFAULT;
        String reason;
        Map.Entry<TicketPriority, Long> top = recent.entrySet().stream()
                .max(Map.Entry.<TicketPriority, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .orElse(null);

        if (total < MIN_TICKETS) {
            reason = "Default priority of " + category.getName() + " (only " + total + " recent ticket"
                    + (total == 1 ? "" : "s") + " in the last 90 days, too few to learn from).";
        } else if (top.getValue() * 2 > total) {
            suggested = top.getKey();
            basis = Basis.HISTORY;
            reason = "Agents chose " + suggested + " for " + top.getValue() + " of " + total + " tickets in "
                    + category.getName() + " in the last 90 days (default " + fallback + ").";
        } else {
            reason = "Default priority of " + category.getName() + " (no single priority was chosen for more than "
                    + "half of the " + total + " recent tickets).";
        }

        return new PrioritySuggestionDto(category.getCode(), category.fullName(), suggested, fallback, basis, reason,
                slaService.targetFor(suggested).toMinutes(), recent);
    }
}
