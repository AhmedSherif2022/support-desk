package com.supportdesk.auth.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest
        (

        @Schema(description = "Case-insensitive.", example = "mona")
        @NotBlank(message = "Username is required.")
        @Size(max = 100, message = "Username is too long.")
        String username,

        @Schema(example = "Password123!")   // the local seed password, so "Try it out" works
        @NotBlank(message = "Password is required.")
        @Size(max = 200, message = "Password is too long.")
        String password
)
{
    @Override
    public String toString()
    {
        return "LoginRequest[username=" + username + ", password=****]";
    }
}