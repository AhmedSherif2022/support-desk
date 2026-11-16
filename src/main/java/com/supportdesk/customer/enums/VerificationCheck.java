package com.supportdesk.customer.enums;
import io.swagger.v3.oas.annotations.media.Schema;

/** Identity checks an agent can tick during a call. At least two must pass (E2). */
@Schema(enumAsRef = true, description = "An identity check the agent asks the caller. At least two must pass.\n"
        + "- DATE_OF_BIRTH\n"
        + "- NATIONAL_ID_LAST4: the last four digits of the national ID\n"
        + "- MOBILE_ON_FILE: the caller is calling from the mobile number on file\n"
        + "- RECENT_TRANSACTION: the caller describes a recent transaction")
public enum VerificationCheck
{
    DATE_OF_BIRTH,
    NATIONAL_ID_LAST4,
    MOBILE_ON_FILE,
    RECENT_TRANSACTION
}
