package com.supportdesk.auth.controller;
import com.supportdesk.auth.dto.JwtResponse;
import com.supportdesk.auth.dto.LoginRequest;
import com.supportdesk.auth.dto.LoginResponse;
import com.supportdesk.auth.dto.MeResponse;
import com.supportdesk.auth.dto.RefreshTokenRequest;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController
{

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public JwtResponse refresh(@RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void logout() {
        authService.logout();
    }


    // "What can I open?": the logged-in user plus the screens their role may see.
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user)
    {
        return MeResponse.from(user);
    }
}
