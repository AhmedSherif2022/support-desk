package com.supportdesk.incident.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;


public class IncidentNotFoundException extends ApiException
{
    public IncidentNotFoundException(Long id)
    {
        super(ErrorCode.INCIDENT_NOT_FOUND, "Incident " + id + " does not exist.");
    }
}
