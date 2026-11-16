package com.supportdesk.customer.enums;
import io.swagger.v3.oas.annotations.media.Schema;

/** Digital-banking enrollment, shown in search results (E2-S1). */
@Schema(enumAsRef = true, description = "Whether the customer uses digital banking.\n"
        + "- ENROLLED\n"
        + "- NOT_ENROLLED\n"
        + "- SUSPENDED: enrolled, but access is blocked")
public enum EnrollmentStatus
{
    ENROLLED,
    NOT_ENROLLED,
    SUSPENDED
}
