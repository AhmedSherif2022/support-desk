package com.supportdesk.auth.controller;

import com.supportdesk.auth.dto.*;
import com.supportdesk.auth.entity.RefreshToken;
import com.supportdesk.auth.exception.InvalidRefreshTokenException;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final CurrentUserService currentUserService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;


    @PostMapping("/refresh")
    public JwtResponse refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest)
    {
        return refreshTokenService.findByToken(refreshTokenRequest.getRefreshToken())
                .map(refreshTokenService::VerifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> JwtResponse.builder()
                        .accessToken(jwtService.generateToken(user).token())
                        .refreshToken(refreshTokenRequest.getRefreshToken())
                        .build())
                .orElseThrow(InvalidRefreshTokenException::new);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request)
    {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthenticatedUser user)
    {
        authService.logout(user.id());
    }

    @GetMapping("/me")
    public UserDto me()
    {
        return UserDto.from(currentUserService.getCurrentUserEntity());
    }
}
