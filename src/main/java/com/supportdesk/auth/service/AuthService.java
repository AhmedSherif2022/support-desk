package com.supportdesk.auth.service;
import com.supportdesk.auth.dto.JwtResponse;
import com.supportdesk.auth.dto.LoginRequest;
import com.supportdesk.auth.dto.LoginResponse;
import com.supportdesk.auth.entity.RefreshToken;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.exception.AccountLockedException;
import com.supportdesk.auth.exception.InvalidCredentialsException;
import com.supportdesk.auth.exception.InvalidRefreshTokenException;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.common.exception.ApiException;
import com.supportdesk.common.exception.UnauthenticatedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {


    private static final String DUMMY_HASH = "$2a$10$a0fbFJX7YiIKsJ0uKU2L1uDf94dfnvf38i9g2KyT7JAHsP3er8hVO";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;



    @Transactional(noRollbackFor = ApiException.class)    // keep the failed-attempt count even when we throw
    public LoginResponse login(LoginRequest request)
    {
        Optional<User> found = userRepository.findByUsernameIgnoreCase(request.username().trim());

        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), DUMMY_HASH); // same time as a real check
            throw new InvalidCredentialsException();                  // 401
        }
        User user = found.get();

        if (!user.isActive()) {
            throw new InvalidCredentialsException();                  // disabled user → same 401
        }

        if (loginAttemptService.isLocked(user)) {
            throw new AccountLockedException(user.getLockedUntil());  // 423 locked
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(user);
            if (loginAttemptService.isLocked(user)) {
                throw new AccountLockedException(user.getLockedUntil());
            }
            throw new InvalidCredentialsException();
        }

        loginAttemptService.recordSuccess(user);                      // reset the failed count
        JwtService.IssuedToken issued = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        // TODO (step 3, audit): publish LoginSucceededEvent / LoginFailedEvent / AccountLockedEvent.
        return new LoginResponse(issued.token(), refreshToken.getToken());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public JwtResponse refresh(String refreshToken)
    {
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::VerifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> JwtResponse.builder()
                        .accessToken(jwtService.generateToken(user).token())
                        .refreshToken(refreshToken)
                        .build())
                .orElseThrow(InvalidRefreshTokenException::new);
    }

    @Transactional
    public void logout()
    {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser badge)) {
            throw new UnauthenticatedException();                 // nobody logged in → 401
        }

        User user = userRepository.findById(badge.id())
                .orElseThrow(UnauthenticatedException::new);
        user.revokeTokens();                                      // old access tokens stop working
        refreshTokenService.deleteByUserId(user.getId());         // refresh token deleted → /refresh fails
    }
}