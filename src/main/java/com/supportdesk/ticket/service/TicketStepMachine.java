package com.supportdesk.ticket.service;

import com.supportdesk.ticket.enums.StepActor;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.enums.TicketStep;
import com.supportdesk.ticket.exception.StepNotYoursException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;


/**
 * The rulebook: who may take which step. Where a step may start is TicketStep itself; TicketStatusMachine guards the move.
 * A step can have more than one actor (the review steps: reviewer or agent), so both methods ask the step itself.
 */
@Component                                   // Spring: one shared instance, injected into TicketService
public class TicketStepMachine
{

    /** The steps this actor can take on a ticket in this status: the ticket's buttons. */
    public List<TicketStep> available(TicketStatus status, StepActor actor)
    {
        return Arrays.stream(TicketStep.values())
                .filter(step -> step.getFrom() == status && step.canBeTakenBy(actor))
                .toList();
    }


    /** 422 STEP_NOT_YOURS when this actor may not take the step. */
    public void checkActor(TicketStep step, StepActor actor)
    {
        if (!step.canBeTakenBy(actor)) {
            throw new StepNotYoursException(step);                     // 422
        }
    }
}
