package com.supportdesk.customer.exception;

import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;


public class CustomerNotFoundException extends ApiException
{
    public CustomerNotFoundException(String cif)
    {
        super(ErrorCode.CUSTOMER_NOT_FOUND, "Customer " + cif + " does not exist.");
    }
}
