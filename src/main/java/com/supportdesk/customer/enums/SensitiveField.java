package com.supportdesk.customer.enums;
import io.swagger.v3.oas.annotations.media.Schema;

/** Fields that stay masked after verification and can be revealed one at a time (E2-S3). */
@Schema(enumAsRef = true, description = "A field that stays masked after verification. The agent reveals it one field at a time, and each reveal is audited.\n"
        + "- NATIONAL_ID\n"
        + "- MOBILE")
public enum SensitiveField
{
    NATIONAL_ID,
    MOBILE
}
