package com.supportdesk.auth.security;


import com.supportdesk.auth.entity.Role;
import com.supportdesk.auth.entity.User;

public record AuthenticatedUser(Long id, String username, Role role)
{
    public static AuthenticatedUser from(User user)
    {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole());
    }
}
