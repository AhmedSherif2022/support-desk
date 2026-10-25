package com.supportdesk.auth.service;

import com.supportdesk.auth.dto.LoginRequest;
import com.supportdesk.auth.dto.LoginResponse;
import com.supportdesk.auth.dto.UserDto;
import com.supportdesk.auth.entity.RefreshToken;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.exception.AccountLockedException;
import com.supportdesk.auth.exception.InvalidCredentialsException;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service

@RequiredArgsConstructor

public class AuthService {


    private static final String DUMMY_HASH = "$2a$10$a0fbFJX7YiIKsJ0uKU2L1uDf94dfnvf38i9g2KyT7JAHsP3er8hVO";

    private final UserRepository userRepository;           // find the user
    private final PasswordEncoder passwordEncoder;         // BCrypt bean from SecurityConfig
    private final LoginAttemptService loginAttemptService; // lockout rule
    private final JwtService jwtService;                   // create the token
    private final RefreshTokenService refreshTokenService; // create refresh token


    @Transactional(noRollbackFor = ApiException.class)

    public LoginResponse login(LoginRequest request)
    {
        Optional<User> found = userRepository.findByUsernameIgnoreCase(request.username().trim());


        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw new InvalidCredentialsException();
        }
        User user = found.get();


        if (!user.isActive())
        {
            throw new InvalidCredentialsException();
        }


        if (loginAttemptService.isLocked(user)) {
            throw new AccountLockedException(user.getLockedUntil());
        }


        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
        {
            loginAttemptService.recordFailure(user);
            if (loginAttemptService.isLocked(user)) {
                throw new AccountLockedException(user.getLockedUntil());
            }
            throw new InvalidCredentialsException();
        }


        loginAttemptService.recordSuccess(user);
        JwtService.IssuedToken issued = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        // TODO (step 3, audit): publish LoginSucceededEvent / LoginFailedEvent / AccountLockedEvent.
        return LoginResponse.bearer(issued.token(), refreshToken.getToken(), issued.expiresAt(), UserDto.from(user));
    }
}