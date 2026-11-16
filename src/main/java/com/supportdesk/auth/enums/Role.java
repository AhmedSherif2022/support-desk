package com.supportdesk.auth.enums;
import io.swagger.v3.oas.annotations.media.Schema;



@Schema(enumAsRef = true, description = "A staff member's role.\n"
        + "- AGENT: takes calls; works customers and tickets\n"
        + "- GUIDE_ADMIN: writes troubleshooting guides; reads the audit log\n"
        + "- REVIEWER: approves guide versions and SRE fixes; reads the audit log")
public enum Role
{

    AGENT,

    GUIDE_ADMIN,

    REVIEWER
}
