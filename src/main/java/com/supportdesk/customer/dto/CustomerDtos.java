package com.supportdesk.customer.dto;
import com.supportdesk.customer.entity.Segment;
import com.supportdesk.customer.entity.VerificationCheck;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.Set;


public final class CustomerDtos
{
    private CustomerDtos()
    {
    }


    public record VerifyCallerRequest(@NotEmpty(message = "Tick the checks that passed.")
                                      Set<VerificationCheck> passedChecks)
    {
    }


    public record VerificationStatusDto(String cif, boolean verified, Set<VerificationCheck> passedChecks,
                                        Instant verifiedAt, Instant expiresAt)
    {
    }


    public record CustomerSummaryDto(String cif, String fullName, Segment segment,
                                     String maskedMobile, String preferredLanguage)
    {
    }
}
