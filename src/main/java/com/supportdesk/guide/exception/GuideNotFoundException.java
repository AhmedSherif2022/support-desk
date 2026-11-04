package com.supportdesk.guide.exception;

import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;

public class GuideNotFoundException extends ApiException
{
    public GuideNotFoundException(Long guideId)
    {
        super(ErrorCode.GUIDE_NOT_FOUND, "Guide " + guideId + " does not exist or has no live version.");
    }
}
