package com.supportdesk.customer.controller;

import com.supportdesk.customer.dto.Customer360Dtos.ActivityResponse;
import com.supportdesk.customer.dto.Customer360Dtos.CustomerDetailDto;
import com.supportdesk.customer.dto.Customer360Dtos.CustomerSearchResponse;
import com.supportdesk.customer.dto.Customer360Dtos.CustomerTicketsDto;
import com.supportdesk.customer.dto.Customer360Dtos.DiagnosisDto;
import com.supportdesk.customer.dto.Customer360Dtos.RevealRequest;
import com.supportdesk.customer.dto.Customer360Dtos.RevealedFieldDto;
import com.supportdesk.customer.dto.CustomerDtos.VerificationStatusDto;
import com.supportdesk.customer.dto.CustomerDtos.VerifyCallerRequest;
import com.supportdesk.customer.enums.ActivityRange;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.customer.service.CallerVerificationService;
import com.supportdesk.customer.service.Customer360Service;
import com.supportdesk.customer.service.CustomerSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;


@RestController
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class CustomerController implements CustomerApi
{
    private final CustomerSearchService searchService;
    private final Customer360Service customer360;
    private final CallerVerificationService verificationService;

    @Override
    public CustomerSearchResponse search(String q)
    {
        return searchService.search(q);
    }

    @Override
    public CustomerDetailDto get(String cif)
    {
        return customer360.detail(cif);
    }

    @Override
    public VerificationStatusDto verify(String cif, VerifyCallerRequest request)
    {
        return verificationService.verify(cif, request.passedChecks());
    }

    @Override
    public void verificationFailed(String cif)
    {
        verificationService.recordFailed(cif);
    }

    @Override
    public VerificationStatusDto status(String cif)
    {
        return verificationService.getStatus(cif);
    }

    @Override
    public void endCall(String cif)
    {
        verificationService.endCall(cif);
    }

    @Override
    public RevealedFieldDto reveal(String cif, RevealRequest request)
    {
        return customer360.reveal(cif, request.field());
    }

    @Override
    public DiagnosisDto diagnosis(String cif)
    {
        return customer360.diagnosis(cif);
    }

    @Override
    public ActivityResponse activity(String cif, ActivityRange range, Instant from, Instant to, Platform platform,
                                     String service, boolean failuresOnly)
    {
        return customer360.activity(cif, range, from, to, platform, service, failuresOnly);
    }

    @Override
    public CustomerTicketsDto tickets(String cif)
    {
        return customer360.tickets(cif);
    }
}
