package com.supportdesk.ticket.service;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.enums.TicketStep;
import com.supportdesk.ticket.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


/**
 * The guard: which status may follow which. Built from TicketStep, so the allowed moves are exactly the arrows of
 * the ticket diagram and can never drift from the steps. Who may take a move is TicketStepMachine's job.
 */
@Component
public class TicketStatusMachine
{

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = new EnumMap<>(TicketStatus.class);

    static {
        for (TicketStatus status : TicketStatus.values()) {
            ALLOWED.put(status, EnumSet.noneOf(TicketStatus.class));   // CLOSED keeps none: final
        }
        for (TicketStep step : TicketStep.values()) {
            ALLOWED.get(step.getFrom()).add(step.getTo());
        }
    }


    public boolean canMove(TicketStatus from, TicketStatus to)
    {
        return ALLOWED.get(from).contains(to);
    }


    public void check(TicketStatus from, TicketStatus to)
    {
        if (!canMove(from, to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }
}
