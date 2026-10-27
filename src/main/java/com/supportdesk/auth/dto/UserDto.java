package com.supportdesk.auth.dto;
import com.supportdesk.auth.entity.Role;
import com.supportdesk.auth.entity.User;



public record UserDto(String username, String fullName, Role role)
{

    public static UserDto from(User user)
    {
        return new UserDto(user.getUsername(), user.getFullName(), user.getRole());
    }
}
