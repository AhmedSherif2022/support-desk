package com.supportdesk.customer.dto;

import com.supportdesk.customer.enums.Segment;
import com.supportdesk.customer.enums.VerificationCheck;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.Set;


public final class CustomerDtos
{
    private CustomerDtos()
    {
    }


    public record VerifyCallerRequest(
            @Schema(description = "At least two identity checks that passed on the call.",
                    example = "[\"DATE_OF_BIRTH\", \"MOBILE_ON_FILE\"]")
            @NotEmpty(message = "Tick the checks that passed.")
            Set<VerificationCheck> passedChecks)
    {
    }


    public record VerificationStatusDto(
            @Schema(example = "CIF-100245") String cif,
            @Schema(example = "true") boolean verified,
            Set<VerificationCheck> passedChecks,
            Instant verifiedAt,
            Instant expiresAt)
    {
    }


}
