package com.supportdesk.ticket.service;

import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.supportdesk.ticket.enums.TicketStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit test: no Spring, no database (fast). */
class TicketStatusMachineTest
{
    private final TicketStatusMachine machine = new TicketStatusMachine();

    /** The agreed ticket diagram, arrow by arrow. If this test fails, the diagram changed. */
    private static final Map<TicketStatus, Set<TicketStatus>> DIAGRAM = Map.of(
            NEW,                 Set.of(TROUBLESHOOTING),
            TROUBLESHOOTING,     Set.of(WAITING_ON_CUSTOMER, RESOLVED, ESCALATED),
            WAITING_ON_CUSTOMER, Set.of(TROUBLESHOOTING),
            ESCALATED,           Set.of(WITH_SRE),
            WITH_SRE,            Set.of(SRE_FIXED),
            SRE_FIXED,           Set.of(IN_REVIEW),
            IN_REVIEW,           Set.of(RESOLVED, REOPENED),
            REOPENED,            Set.of(WITH_SRE),
            RESOLVED,            Set.of(CLOSED),
            CLOSED,              Set.of());

    @Test
    void allowedMovesAreExactlyTheDiagram()
    {
        for (TicketStatus from : TicketStatus.values()) {
            for (TicketStatus to : TicketStatus.values()) {
                assertThat(machine.canMove(from, to)).as(from + " -> " + to).isEqualTo(DIAGRAM.get(from).contains(to));
            }
        }
    }

    @Test
    void closedIsFinal()
    {
        assertThatThrownBy(() -> machine.check(CLOSED, NEW)).isInstanceOf(InvalidStatusTransitionException.class);
    }
}
