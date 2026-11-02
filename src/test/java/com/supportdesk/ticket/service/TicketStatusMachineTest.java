package com.supportdesk.ticket.service;

import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit test: no Spring, no database (fast). */
class TicketStatusMachineTest
{
    private final TicketStatusMachine machine = new TicketStatusMachine();

    @Test
    void normalFlowIsAllowed()
    {
        assertThat(machine.canMove(TicketStatus.NEW, TicketStatus.TROUBLESHOOTING)).isTrue();
        assertThat(machine.canMove(TicketStatus.TROUBLESHOOTING, TicketStatus.WAITING_ON_CUSTOMER)).isTrue();
        assertThat(machine.canMove(TicketStatus.WAITING_ON_CUSTOMER, TicketStatus.RESOLVED)).isTrue();
        assertThat(machine.canMove(TicketStatus.RESOLVED, TicketStatus.CLOSED)).isTrue();
    }

    @Test
    void resolvedCanBeReopened()
    {
        assertThat(machine.canMove(TicketStatus.RESOLVED, TicketStatus.REOPENED)).isTrue();
    }

    @Test
    void sreFixCanBeConfirmedOrRejected()
    {
        assertThat(machine.canMove(TicketStatus.SRE_FIXED, TicketStatus.RESOLVED)).isTrue();
        assertThat(machine.canMove(TicketStatus.SRE_FIXED, TicketStatus.WITH_SRE)).isTrue();
    }

    @Test
    void closedIsFinal()
    {
        for (TicketStatus to : TicketStatus.values()) {
            assertThat(machine.canMove(TicketStatus.CLOSED, to)).isFalse();
        }
        assertThatThrownBy(() -> machine.check(TicketStatus.CLOSED, TicketStatus.NEW))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void agentCannotChooseSystemStatuses()
    {
        assertThat(machine.isAgentChoice(TicketStatus.WITH_SRE)).isFalse();
        assertThat(machine.isAgentChoice(TicketStatus.SRE_FIXED)).isFalse();
        assertThat(machine.agentChoicesFrom(TicketStatus.NEW))
                .contains(TicketStatus.RESOLVED)
                .doesNotContain(TicketStatus.ESCALATED);
    }
}
