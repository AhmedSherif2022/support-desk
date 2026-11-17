package com.supportdesk.guide.exception;

import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;

public class GuideFeedbackNotFoundException extends ApiException
{
    public GuideFeedbackNotFoundException(Long id)
    {
        super(ErrorCode.GUIDE_FEEDBACK_NOT_FOUND, "Feedback " + id + " does not exist.");
    }
}
