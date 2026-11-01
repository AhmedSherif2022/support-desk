package com.supportdesk.auth.controller;
import com.supportdesk.auth.dto.JwtResponse;
import com.supportdesk.auth.dto.LoginRequest;
import com.supportdesk.auth.dto.LoginResponse;
import com.supportdesk.auth.dto.MeResponse;
import com.supportdesk.auth.dto.RefreshTokenRequest;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi
{

    private final AuthService authService;

    @Override
    public LoginResponse login(LoginRequest request) {
        return authService.login(request);
    }

    @Override
    public JwtResponse refresh(RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @Override
    public void logout() {
        authService.logout();
    }

    @Override
    public MeResponse me(AuthenticatedUser user)
    {
        return MeResponse.from(user);
    }
}
