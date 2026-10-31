package com.supportdesk.auth.dto;
import com.supportdesk.auth.entity.Role;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.security.Permissions;

import java.util.List;


/**
 * GET /api/auth/me: who is logged in, and which screens the frontend may show them.
 * Built from the token's badge, so it needs no database call.
 */
public record MeResponse(Long id, String username, Role role, List<String> allowedScreens)
{
    public static MeResponse from(AuthenticatedUser user)
    {
        return new MeResponse(user.id(), user.username(), user.role(), Permissions.screensFor(user.role()));
    }
}
