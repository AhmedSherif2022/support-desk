package com.supportdesk.customer.controller;

import com.supportdesk.common.exception.ApiError;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Instant;

/**
 * HTTP contract for customer search, caller verification and Customer 360 (E2).
 * {@link CustomerController} only implements this and delegates.
 */
@RequestMapping("/api/customers")
@PreAuthorize("hasRole('AGENT')")
@Tag(name = "Customers", description = "Find the caller, verify them, then open Customer 360. "
        + "Everything after search needs a verified caller; sensitive fields stay masked until revealed.")
public interface CustomerApi
{
    @GetMapping("/search")
    @Operation(summary = "Search customers (E2-S1)",
            description = "One box for CIF, national ID, mobile (01…, 20… or +20…) or ticket number (#2001). "
                    + "Results are masked. Audited as CUSTOMER_SEARCHED without the search text. "
                    + "More than 30 searches in 10 minutes answer 429.")
    @ApiResponse(responseCode = "200", description = "Matches, or an empty list with \"No match. Check the digits and the ID type.\"")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: only agents search customers",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "429", description = "TOO_MANY_SEARCHES: more than 30 searches in 10 minutes",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CustomerSearchResponse search(
            @Parameter(description = "CIF, national ID, mobile or ticket number.", example = "CIF-100245")
            @RequestParam String q);

    @GetMapping("/{cif}")
    @Operation(summary = "Customer 360 header (E2-S2/S3)",
            description = "Needs a verified caller. National ID shows the last 4 digits, mobile the first 3 and last 4. "
                    + "Audited as CUSTOMER_DATA_VIEWED.")
    @ApiResponse(responseCode = "200", description = "The customer, masked")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "CALLER_NOT_VERIFIED, or FORBIDDEN for non-agents",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND: unknown CIF",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CustomerDetailDto get(
            @Parameter(description = "Customer CIF.", example = "CIF-100245")
            @PathVariable String cif);

    @PostMapping("/{cif}/verify")
    @Operation(summary = "Verify caller identity (E2-S2)",
            description = "Two or more checks must pass. Audited as CALLER_VERIFIED. Valid for 30 minutes or until end-call.")
    @ApiResponse(responseCode = "200", description = "Caller verified")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or MALFORMED_REQUEST",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: this role cannot verify callers",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND: unknown CIF",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "TOO_FEW_CHECKS: \"Two checks must pass.\"",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    VerificationStatusDto verify(
            @Parameter(description = "Customer CIF.", example = "CIF-100245")
            @PathVariable String cif,
            @Valid @RequestBody VerifyCallerRequest request);

    @PostMapping("/{cif}/verification-failed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Caller failed verification (E2-S2)",
            description = "Ends any verification for this agent and customer and audits CALLER_VERIFICATION_FAILED. "
                    + "No customer data is returned.")
    @ApiResponse(responseCode = "204", description = "Recorded")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    void verificationFailed(
            @Parameter(description = "Customer CIF.", example = "CIF-100245")
            @PathVariable String cif);

    @GetMapping("/{cif}/verification")
    @Operation(summary = "Check caller verification status",
            description = "Whether the current agent has an active caller verification for this CIF.")
    @ApiResponse(responseCode = "200", description = "Current verification status")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    VerificationStatusDto status(
            @Parameter(description = "Customer CIF.", example = "CIF-100245")
            @PathVariable String cif);

    @PostMapping("/{cif}/end-call")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "End verification (E2-S2)",
            description = "Locks the customer again for this agent: every Customer 360 call answers CALLER_NOT_VERIFIED until verified again.")
    @ApiResponse(responseCode = "204", description = "Verification ended")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    void endCall(
            @Parameter(description = "Customer CIF.", example = "CIF-100245")
            @PathVariable String cif);

    @PostMapping("/{cif}/reveal")
    @Operation(summary = "Reveal one masked field (E2-S3)",
            description = "Returns the full value of one field. Audited as CUSTOMER_FIELD_REVEALED with the field name (not the value).")
    @ApiResponse(responseCode = "200", description = "The full value of that one field")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: field is missing or not NATIONAL_ID / MOBILE",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "CALLER_NOT_VERIFIED, or FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    RevealedFieldDto reveal(
            @Parameter(description = "Customer CIF.", example = "CIF-100245")
            @PathVariable String cif,
            @Valid @RequestBody RevealRequest request);

    @GetMapping("/{cif}/diagnosis")
    @Operation(summary = "What the logs already say (E2-S4)",
            description = "The most frequent failure of the last 6 hours (count, event type, error code, service, time range), "
                    + "and the services with no failures. With no failures, suggests checking device or network.")
    @ApiResponse(responseCode = "200", description = "The diagnosis")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "CALLER_NOT_VERIFIED, or FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    DiagnosisDto diagnosis(
            @Parameter(description = "Customer CIF.", example = "CIF-100245")
            @PathVariable String cif);

    @GetMapping("/{cif}/activity")
    @Operation(summary = "Activity timeline (E2-S5)",
            description = "Newest first. Default: last 6 hours, failures only. Counts cover the time range and result filter, "
                    + "so every platform and service chip can show its number.")
    @ApiResponse(responseCode = "200", description = "Matching events, counts, and an empty-state hint when nothing matches")
    @ApiResponse(responseCode = "400", description = "CUSTOM_RANGE_REQUIRED: CUSTOM needs from before to; or REQUEST_ERROR for a bad enum",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "CALLER_NOT_VERIFIED, or FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    ActivityResponse activity(
            @Parameter(description = "Customer CIF.", example = "CIF-100245") @PathVariable String cif,
            @Parameter(description = "LAST_6_HOURS (default), TODAY, LAST_7_DAYS or CUSTOM.")
            @RequestParam(required = false) ActivityRange range,
            @Parameter(description = "CUSTOM only: start (inclusive), ISO-8601.", example = "2026-11-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "CUSTOM only: end (exclusive), ISO-8601.", example = "2026-11-03T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "Platform chip. Omit to clear it.") @RequestParam(required = false) Platform platform,
            @Parameter(description = "Service chip, e.g. AUTHENTICATION. Omit to clear it.", example = "AUTHENTICATION")
            @RequestParam(required = false) String service,
            @Parameter(description = "true (default) shows failures only.", example = "true")
            @RequestParam(defaultValue = "true") boolean failuresOnly);

    @GetMapping("/{cif}/tickets")
    @Operation(summary = "Customer's tickets (E2-S6)",
            description = "Newest first, with origin and status. More than one ticket in 20 days adds a \"Repeat caller\" note.")
    @ApiResponse(responseCode = "200", description = "The customer's tickets")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "CALLER_NOT_VERIFIED, or FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    CustomerTicketsDto tickets(
            @Parameter(description = "Customer CIF.", example = "CIF-100245")
            @PathVariable String cif);
}
