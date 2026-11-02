package com.supportdesk.customer.controller;
import com.supportdesk.customer.dto.CustomerDtos.CustomerSummaryDto;
import com.supportdesk.customer.dto.CustomerDtos.VerificationStatusDto;
import com.supportdesk.customer.dto.CustomerDtos.VerifyCallerRequest;
import com.supportdesk.customer.service.CallerVerificationService;
import com.supportdesk.customer.service.CustomerLookupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/customers")
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class CustomerController
{
    private final CustomerLookupService customerLookup;
    private final CallerVerificationService verificationService;


    @GetMapping("/{cif}")
    public CustomerSummaryDto get(@PathVariable String cif)
    {
        return customerLookup.getSummary(cif);
    }


    @PostMapping("/{cif}/verify")
    public VerificationStatusDto verify(@PathVariable String cif, @Valid @RequestBody VerifyCallerRequest request)
    {
        return verificationService.verify(cif, request.passedChecks());
    }


    @GetMapping("/{cif}/verification")
    public VerificationStatusDto status(@PathVariable String cif)
    {
        return verificationService.getStatus(cif);
    }


    @PostMapping("/{cif}/end-call")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endCall(@PathVariable String cif)
    {
        verificationService.endCall(cif);
    }
}
