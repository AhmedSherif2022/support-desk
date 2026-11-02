package com.supportdesk.ticket.config;

import com.supportdesk.ticket.enums.TicketPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

/**
 * SLA clock lengths, bound from supportdesk.tickets in application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "supportdesk.tickets")
public record TicketProperties(@NotNull @Valid Sla sla)
{
    private static final Duration MIN_TARGET = Duration.ofSeconds(1);

    public record Sla(
            @Min(1) @Max(99) int warningAtPercent,
            @NotEmpty Map<TicketPriority, @NotNull Duration> targets)
    {
        public Sla
        {
            EnumSet<TicketPriority> missing = EnumSet.allOf(TicketPriority.class);
            if (targets != null) {
                missing.removeAll(targets.keySet());
            }
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("SLA target missing for " + missing);
            }
            for (TicketPriority priority : TicketPriority.values()) {
                Duration target = targets.get(priority);
                if (target == null || target.compareTo(MIN_TARGET) < 0) {
                    throw new IllegalArgumentException("SLA target for " + priority + " must be at least 1 second.");
                }
            }
            targets = Collections.unmodifiableMap(new EnumMap<>(targets));
        }

        public Duration targetFor(TicketPriority priority)
        {
            Objects.requireNonNull(priority, "priority must not be null");
            return targets.get(priority);
        }
    }
}
