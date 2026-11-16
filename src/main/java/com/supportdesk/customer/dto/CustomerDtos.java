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
            @Schema(description = "The checks that passed; empty when not verified.", example = "[\"DATE_OF_BIRTH\", \"MOBILE_ON_FILE\"]") Set<VerificationCheck> passedChecks,
            @Schema(description = "When the caller was verified; null when not verified.", example = "2026-11-16T10:30:00Z") Instant verifiedAt,
            @Schema(description = "When the verification lapses: 30 minutes after verifiedAt, unless the call is ended first. Null when not verified.", example = "2026-11-16T11:00:00Z") Instant expiresAt)
    {
    }


}
