package com.supportdesk.auth.controller;

import com.supportdesk.auth.dto.*;
import com.supportdesk.auth.entity.RefreshToken;
import com.supportdesk.auth.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthController
{

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final RegistrationService registrationService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public AuthController(AuthService authService,
                          CurrentUserService currentUserService,
                          RegistrationService registrationService
                         ,RefreshTokenService refreshTokenService
                          ,JwtService jwtService)
    {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.registrationService = registrationService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    @PostMapping("/registration")
    public ResponseEntity<String> registration
            (@RequestBody @Valid RegistrationRequest registrationRequest){
        boolean isSaved = registrationService.createAccount(registrationRequest);
        if (isSaved ){
            return ResponseEntity.status(HttpStatus.CREATED).body("Your Account Created Succsfully");
        }else
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Information");
    }
    @PostMapping("/refresh")
    public JwtResponse refreshToken(@RequestBody RefreshTokenRequest refreshTokenReuest) {
        return refreshTokenService.findByToken(refreshTokenReuest.getToken())
                .map(refreshTokenService::VerifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateToken(user).token();
                    return JwtResponse.builder()
                            .accessToken(accessToken)
                            .token(refreshTokenReuest.getToken())
                            .build();
                }).orElseThrow(() -> new RuntimeException("Invalid Refresh Token"));
    }


    @PostMapping("/login")
    public LoginResponse login(
            @Valid
            @RequestBody
            LoginRequest request)
    {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout()
    {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserDto me() {
        return UserDto.from(currentUserService.getCurrentUserEntity());
    }
}
