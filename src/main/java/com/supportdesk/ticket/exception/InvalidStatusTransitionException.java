package com.supportdesk.ticket.exception;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.ticket.enums.TicketStatus;


public class InvalidStatusTransitionException extends BusinessRuleException
{
    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to)
    {
        super(ErrorCode.INVALID_STATUS_TRANSITION,
                "A ticket cannot move from " + from + " to " + to + ".");
    }
}
