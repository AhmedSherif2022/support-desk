package com.supportdesk.auth.dto;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.security.Permissions;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;


/**
 * GET /api/auth/me: who is logged in, and which screens the frontend may show them.
 * Built from the token's badge, so it needs no database call.
 */
public record MeResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "mona") String username,
        @Schema(example = "AGENT") Role role,
        @Schema(description = "Screens this role may open, in menu order. AGENT: HOME, CUSTOMER_SEARCH, MY_TICKETS, "
                + "NOTIFICATIONS (a menu item with no API yet).",
                example = "[\"HOME\", \"CUSTOMER_SEARCH\", \"MY_TICKETS\", \"NOTIFICATIONS\"]")
        List<String> allowedScreens)
{
    public static MeResponse from(AuthenticatedUser user)
    {
        return new MeResponse(user.id(), user.username(), user.role(), Permissions.screensFor(user.role()));
    }
}
