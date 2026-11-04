package com.supportdesk.guide.exception;

import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;

public class GuideRunNotFoundException extends ApiException
{
    public GuideRunNotFoundException(Long runId)
    {
        super(ErrorCode.GUIDE_RUN_NOT_FOUND, "Guide run " + runId + " does not exist.");
    }
}
