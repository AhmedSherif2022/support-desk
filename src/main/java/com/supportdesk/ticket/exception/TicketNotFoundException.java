package com.supportdesk.ticket.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;


public class TicketNotFoundException extends ApiException
{
    public TicketNotFoundException(Long ticketNumber)
    {
        super(ErrorCode.TICKET_NOT_FOUND, "Ticket #" + ticketNumber + " does not exist.");
    }
}
