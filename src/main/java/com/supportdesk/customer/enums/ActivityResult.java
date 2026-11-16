package com.supportdesk.customer.enums;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Whether a customer's action in the bank's systems succeeded.\n"
        + "- SUCCESS\n"
        + "- FAILURE: carries an error code")
public enum ActivityResult
{
    SUCCESS,
    FAILURE
}
