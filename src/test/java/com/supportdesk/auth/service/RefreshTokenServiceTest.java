package com.supportdesk.auth.service;

import com.supportdesk.auth.config.AuthProperties;
import com.supportdesk.auth.entity.RefreshToken;
import com.supportdesk.auth.repository.RefreshTokenRepository;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.exception.RefreshTokenExpiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest
{

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthProperties authProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void expiredTokenIsDeletedAndRejectedWith401()
    {
        RefreshToken expired = tokenExpiringIn(Duration.ofMinutes(-1));

        assertThatThrownBy(() -> refreshTokenService.VerifyExpiration(expired))
                .isInstanceOf(RefreshTokenExpiredException.class)
                .extracting(ex -> ((RefreshTokenExpiredException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void validTokenIsReturned()
    {
        RefreshToken valid = tokenExpiringIn(Duration.ofMinutes(30));

        assertThat(refreshTokenService.VerifyExpiration(valid)).isSameAs(valid);
        verify(refreshTokenRepository, never()).delete(valid);
    }

    @Test
    void extendPushesTheExpiryToTheInactivityTimeout()
    {
        when(authProperties.inactivityTimeout()).thenReturn(Duration.ofMinutes(30));
        RefreshToken nearlyIdle = tokenExpiringIn(Duration.ofMinutes(1));

        refreshTokenService.extend(nearlyIdle);

        assertThat(nearlyIdle.getExpirydate()).isAfter(Instant.now().plus(Duration.ofMinutes(29)));
    }

    private RefreshToken tokenExpiringIn(Duration fromNow)
    {
        return RefreshToken.builder()
                .token("refresh-token")
                .expirydate(Instant.now().plus(fromNow))
                .build();
    }
}
