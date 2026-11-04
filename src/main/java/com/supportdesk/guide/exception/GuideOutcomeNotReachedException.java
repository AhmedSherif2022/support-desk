package com.supportdesk.guide.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;

public class GuideOutcomeNotReachedException extends ApiException
{
    public GuideOutcomeNotReachedException(Long runId)
    {
        super(ErrorCode.GUIDE_OUTCOME_NOT_REACHED, "Guide run " + runId + " has not reached an outcome to complete.");
    }
}
