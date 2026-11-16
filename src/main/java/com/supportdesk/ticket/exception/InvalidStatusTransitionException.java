package com.supportdesk.ticket.exception;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.enums.TicketStep;


public class InvalidStatusTransitionException extends BusinessRuleException
{
    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to)
    {
        super(ErrorCode.INVALID_STATUS_TRANSITION,
                "A ticket cannot move from " + from + " to " + to + ".");
    }

    public InvalidStatusTransitionException(TicketStep step, TicketStatus current)
    {
        super(ErrorCode.INVALID_STATUS_TRANSITION,
                step + " starts from " + step.getFrom() + ", but the ticket is " + current + ".");
    }
}
