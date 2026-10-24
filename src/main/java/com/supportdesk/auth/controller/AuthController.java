package com.supportdesk.auth.controller;

import com.supportdesk.auth.dto.LoginRequest;
import com.supportdesk.auth.dto.LoginResponse;
import com.supportdesk.auth.dto.UserDto;
import com.supportdesk.auth.service.AuthService;
import com.supportdesk.auth.service.CurrentUserService;
import jakarta.validation.Valid;
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


    public AuthController(AuthService authService, CurrentUserService currentUserService)
    {
        this.authService = authService;
        this.currentUserService = currentUserService;
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
        authService.logout(currentUserService.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserDto me() {
        return UserDto.from(currentUserService.getCurrentUserEntity());
    }
}
