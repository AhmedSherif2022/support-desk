package com.supportdesk.customer.enums;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(enumAsRef = true, description = "Where the customer used the bank.\n"
        + "- MOBILE: the mobile app\n"
        + "- WEB: web banking\n"
        + "- BRANCH: at a branch")
public enum Platform
{
    MOBILE,
    WEB,
    BRANCH
}
