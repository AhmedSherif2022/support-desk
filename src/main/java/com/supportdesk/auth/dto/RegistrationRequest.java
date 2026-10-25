package com.supportdesk.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank@Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Invalid mobile number")
        String mobileNumber,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
