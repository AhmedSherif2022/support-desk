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

        @Schema(description = "SHA-256 of the password as 64 hex characters, made in the browser, never the password "
                + "itself. The local profile also accepts the plain password.",
                example = "a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea")   // SHA-256 of the local seed password Password123!
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