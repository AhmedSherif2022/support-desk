package com.supportdesk.ticket.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.ticket.enums.TicketStatus;


public class StatusSetBySystemException extends BusinessRuleException
{
    public StatusSetBySystemException(TicketStatus status)
    {
        super(ErrorCode.STATUS_SET_BY_SYSTEM, status + " is set by the system, not by hand.");
    }
}
