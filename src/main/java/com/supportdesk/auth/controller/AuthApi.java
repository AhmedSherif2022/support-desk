package com.supportdesk.auth.controller;

import com.supportdesk.auth.dto.JwtResponse;
import com.supportdesk.auth.dto.LoginRequest;
import com.supportdesk.auth.dto.LoginResponse;
import com.supportdesk.auth.dto.MeResponse;
import com.supportdesk.auth.dto.RefreshTokenRequest;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * HTTP contract for auth: paths, status codes, and who may call them.
 * {@link AuthController} only implements this and delegates.
 */
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Log in, renew the access token, log out, and ask \"what can I open?\"")
public interface AuthApi
{
    @PostMapping("/login")
    @SecurityRequirements   // public: no token needed
    @Operation(summary = "Log in",
            description = "Returns a short-lived access token and a refresh token. "
                    + "Too many wrong passwords in a row lock the account for a while.")
    @ApiResponse(responseCode = "200", description = "Logged in")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: username or password missing or too long",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS: wrong username or password (same answer for both)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "423", description = "ACCOUNT_LOCKED: too many failed logins; the message says until when",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    LoginResponse login(@Valid @RequestBody LoginRequest request);

    @PostMapping("/refresh")
    @SecurityRequirements   // public: the refresh token in the body is the credential
    @Operation(summary = "Get a new access token",
            description = "Trade the refresh token from login for a fresh access token. Fails after logout.")
    @ApiResponse(responseCode = "200", description = "New access token (the refresh token is returned unchanged)")
    @ApiResponse(responseCode = "401", description = "INVALID_REFRESH_TOKEN (unknown or logged out) or REFRESH_TOKEN_EXPIRED",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    JwtResponse refresh(@RequestBody RefreshTokenRequest request);

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Log out",
            description = "Ends the session everywhere: every access token issued so far stops working "
                    + "and the refresh token is deleted.")
    @ApiResponse(responseCode = "204", description = "Logged out")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it already expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    void logout();

    // "What can I open?": the logged-in user plus the screens their role may see.
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Who am I, and what can I open?",
            description = "The logged-in user and the screens their role may see. "
                    + "The frontend builds its menu from allowedScreens.")
    @ApiResponse(responseCode = "200", description = "The logged-in user")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    MeResponse me(@AuthenticationPrincipal AuthenticatedUser user);
}
