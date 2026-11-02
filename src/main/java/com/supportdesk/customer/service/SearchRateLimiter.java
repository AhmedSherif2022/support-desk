package com.supportdesk.customer.service;

import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * E1-S4: more than 30 customer searches by one agent in 10 minutes are refused with 429.
 * shortcut: counts live in this app instance's memory; move them to a shared store (Redis, DB) before running
 * more than one instance, or each instance allows 30.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchRateLimiter
{
    static final int MAX_SEARCHES = 30;
    static final Duration WINDOW = Duration.ofMinutes(10);

    private final Clock clock;
    private final Map<Long, Deque<Instant>> recentSearches = new ConcurrentHashMap<>();

    public void check(Long agentId)
    {
        Instant now = clock.instant();
        Deque<Instant> searches = recentSearches.computeIfAbsent(agentId, id -> new ArrayDeque<>());
        synchronized (searches) {
            while (!searches.isEmpty() && !searches.peekFirst().isAfter(now.minus(WINDOW))) {
                searches.pollFirst();
            }
            if (searches.size() >= MAX_SEARCHES) {
                // TODO (E9 notifications): tell the agent's Team Leader once the role and notifications exist.
                log.warn("Search rate limit hit: agentId={}, searches in last {} = {}", agentId, WINDOW, searches.size());
                throw new ApiException(ErrorCode.TOO_MANY_SEARCHES);
            }
            searches.addLast(now);
        }
    }
}
