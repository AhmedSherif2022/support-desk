package com.supportdesk.ticket.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.ticket.enums.TicketStep;


public class StepNotYoursException extends BusinessRuleException
{
    public StepNotYoursException(TicketStep step)
    {
        super(ErrorCode.STEP_NOT_YOURS, step + " can only be taken by " + step.getActor() + ".");
    }
}
