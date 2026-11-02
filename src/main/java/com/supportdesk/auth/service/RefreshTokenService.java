package com.supportdesk.auth.service;

import com.supportdesk.auth.config.AuthProperties;
import com.supportdesk.auth.entity.RefreshToken;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.repository.RefreshTokenRepository;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.exception.RefreshTokenExpiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService
{

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AuthProperties authProperties;

    @Transactional
    public RefreshToken createRefreshToken(String username)
    {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElseGet(() -> RefreshToken.builder().user(user).build());

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpirydate(Instant.now().plus(authProperties.inactivityTimeout()));
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void deleteByUserId(Long userId)
    {
        refreshTokenRepository.deleteByUserId(userId);
    }

    public Optional<RefreshToken> findByToken(String token)
    {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken VerifyExpiration(RefreshToken token)
    {
        if (token.getExpirydate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenExpiredException();
        }
        return token;
    }

    // Sliding expiry: each refresh pushes the end out, so only inactivity ends the session (E1-S1).
    // shortcut: activity is seen only at refresh time, so idle time can be off by up to one access-token
    // lifetime; track per-request activity if the 30 minutes must be exact.
    public RefreshToken extend(RefreshToken token)
    {
        token.setExpirydate(Instant.now().plus(authProperties.inactivityTimeout()));
        return token;
    }

}
