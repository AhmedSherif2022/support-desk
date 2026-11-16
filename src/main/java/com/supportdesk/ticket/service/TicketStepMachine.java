package com.supportdesk.ticket.service;
import com.supportdesk.ticket.enums.StepActor;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.enums.TicketStep;
import com.supportdesk.ticket.exception.StepNotYoursException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;


/** The rulebook: who may take which step. Where a step may start is TicketStep itself; TicketStatusMachine guards the move. */
@Component
public class TicketStepMachine
{

    /** The steps this actor can take on a ticket in this status: the ticket's buttons. */
    public List<TicketStep> available(TicketStatus status, StepActor actor)
    {
        return Arrays.stream(TicketStep.values())
                .filter(step -> step.getFrom() == status && step.getActor() == actor)
                .toList();
    }


    public void checkActor(TicketStep step, StepActor actor)
    {
        if (step.getActor() != actor) {
            throw new StepNotYoursException(step);                     // 422
        }
    }
}
