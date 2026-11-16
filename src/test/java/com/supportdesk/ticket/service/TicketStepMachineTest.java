package com.supportdesk.ticket.service;

import com.supportdesk.ticket.enums.StepActor;
import com.supportdesk.ticket.enums.TicketStep;
import com.supportdesk.ticket.exception.StepNotYoursException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.supportdesk.ticket.enums.TicketStatus.IN_REVIEW;
import static com.supportdesk.ticket.enums.TicketStatus.REOPENED;
import static com.supportdesk.ticket.enums.TicketStatus.TROUBLESHOOTING;
import static com.supportdesk.ticket.enums.TicketStep.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketStepMachineTest
{
    private final TicketStepMachine machine = new TicketStepMachine();

    @Test
    void theDiagramHasFifteenStepsByWhoTakesThem()
    {
        assertThat(Arrays.stream(TicketStep.values())
                .collect(Collectors.groupingBy(TicketStep::getActor, Collectors.counting())))
                .isEqualTo(Map.of(StepActor.AGENT, 5L, StepActor.GUIDE, 3L, StepActor.SYSTEM, 5L, StepActor.REVIEWER, 2L));
    }

    @Test
    void eachActorSeesOnlyItsOwnStepsFromTheStatus()
    {
        assertThat(machine.available(TROUBLESHOOTING, StepActor.AGENT)).containsExactly(REQUEST_DETAILS, APPLY_FIX);
        assertThat(machine.available(TROUBLESHOOTING, StepActor.GUIDE))
                .containsExactly(GUIDE_SOLVED, GUIDE_FOLLOW_UP, GUIDE_HAND_OVER);
        assertThat(machine.available(IN_REVIEW, StepActor.REVIEWER)).containsExactly(REVIEWER_APPROVES, REVIEWER_REJECTS);
        assertThat(machine.available(IN_REVIEW, StepActor.AGENT)).isEmpty();
        assertThat(machine.available(REOPENED, StepActor.SYSTEM)).containsExactly(FIX_FAILED);
        assertThat(machine.available(TROUBLESHOOTING, null)).isEmpty();          // GUIDE_ADMIN takes no steps
    }

    @Test
    void someoneElsesStepIsRefused()
    {
        assertThatThrownBy(() -> machine.checkActor(HAND_OVER, StepActor.AGENT)).isInstanceOf(StepNotYoursException.class);
        assertThatThrownBy(() -> machine.checkActor(REVIEWER_APPROVES, StepActor.AGENT)).isInstanceOf(StepNotYoursException.class);
        assertThatThrownBy(() -> machine.checkActor(APPLY_FIX, StepActor.REVIEWER)).isInstanceOf(StepNotYoursException.class);
        machine.checkActor(APPLY_FIX, StepActor.AGENT);
    }
}
