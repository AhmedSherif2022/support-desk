package com.supportdesk.ticket.exception;

import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;
import lombok.Getter;

@Getter                                   // Lombok: getExistingTicketNumber()
public class DuplicateTicketException extends ApiException
{
    private final Long existingTicketNumber;

    public DuplicateTicketException(Long existingTicketNumber)
    {
        super(ErrorCode.DUPLICATE_OPEN_TICKET,
                "Ticket #" + existingTicketNumber + " is already open for this problem. Open it instead?");
        this.existingTicketNumber = existingTicketNumber;
    }
}