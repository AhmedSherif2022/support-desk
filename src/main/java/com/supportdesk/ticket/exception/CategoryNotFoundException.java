package com.supportdesk.ticket.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;
import com.supportdesk.common.exception.BusinessRuleException;


    public class CategoryNotFoundException extends BusinessRuleException
    {
        public CategoryNotFoundException(String categoryCode)
        {
            super(ErrorCode.CATEGORY_NOT_FOUND, "Category " + categoryCode + " does not exist.");
        }
    }

