package com.supportdesk.ticket.enums;

import com.supportdesk.ticket.dto.SlaSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** E3-S3 SLA label and E3-S5 next step, without Spring. */
class TicketRulesTest
{
    // ---- E3-S5: exactly one next step per status ----

    @Test
    void everyStatusNamesItsNextStep()
    {
        assertThat(NextAction.resolve(TicketStatus.NEW, SlaState.ON_TRACK)).isEqualTo(NextAction.START_GUIDE);
        assertThat(NextAction.resolve(TicketStatus.REOPENED, SlaState.ON_TRACK)).isEqualTo(NextAction.START_GUIDE);
        assertThat(NextAction.resolve(TicketStatus.TROUBLESHOOTING, SlaState.ON_TRACK)).isEqualTo(NextAction.ESCALATE);
        assertThat(NextAction.resolve(TicketStatus.WAITING_ON_CUSTOMER, SlaState.PAUSED)).isEqualTo(NextAction.RESUME);
        assertThat(NextAction.resolve(TicketStatus.ESCALATED, SlaState.ON_TRACK)).isEqualTo(NextAction.WAIT_FOR_SRE);
        assertThat(NextAction.resolve(TicketStatus.WITH_SRE, SlaState.ON_TRACK)).isEqualTo(NextAction.WAIT_FOR_SRE);
        assertThat(NextAction.resolve(TicketStatus.WITH_SRE, SlaState.AT_RISK)).isEqualTo(NextAction.ASK_SRE_FOR_UPDATE);
        assertThat(NextAction.resolve(TicketStatus.WITH_SRE, SlaState.BREACHED)).isEqualTo(NextAction.ASK_SRE_FOR_UPDATE);
        assertThat(NextAction.resolve(TicketStatus.SRE_FIXED, SlaState.ON_TRACK)).isEqualTo(NextAction.CALL_AND_CONFIRM);
        assertThat(NextAction.resolve(TicketStatus.RESOLVED, SlaState.MET)).isEqualTo(NextAction.CLOSE);
        assertThat(NextAction.resolve(TicketStatus.CLOSED, SlaState.MET)).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = "CLOSED", mode = EnumSource.Mode.EXCLUDE)
    void openOrResolvedTicketsAlwaysHaveAStep(TicketStatus status)
    {
        assertThat(NextAction.resolve(status, SlaState.ON_TRACK)).isNotNull();
    }

    // ---- E3-S3: one SLA value ----

    @Test
    void labelShowsTimeLeftPausedMetOrHowLongItIsBreached()
    {
        assertThat(snapshot(SlaState.ON_TRACK, Duration.ofMinutes(90)).label()).isEqualTo("1h 30m left");
        assertThat(snapshot(SlaState.AT_RISK, Duration.ofMinutes(25)).label()).isEqualTo("0h 25m left");
        assertThat(snapshot(SlaState.PAUSED, Duration.ofHours(3)).label()).isEqualTo("Paused");
        assertThat(snapshot(SlaState.MET, Duration.ofHours(1)).label()).isEqualTo("Met");
        assertThat(snapshot(SlaState.BREACHED, Duration.ofMinutes(-75)).label()).isEqualTo("Breached 1h 15m");
    }

    private static SlaSnapshot snapshot(SlaState state, Duration remaining)
    {
        return new SlaSnapshot(state, 50, Duration.ofHours(1), Duration.ofHours(2), remaining, state == SlaState.PAUSED);
    }
}
