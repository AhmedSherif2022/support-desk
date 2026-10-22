package com.supportdesk.auth.service;
import com.supportdesk.auth.config.AuthProperties;
import com.supportdesk.auth.entity.User;
import org.springframework.stereotype.Service;
import java.time.Clock;


@Service
public class LoginAttemptService
{

    private final AuthProperties properties;
    private final Clock clock;

    public LoginAttemptService(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }


    public boolean isLocked(User user)
    {
        return user.isLocked(clock.instant());
    }


    public void recordFailure(User user) {
        user.registerFailedLogin();
        if (user.getFailedLoginAttempts() >= properties.maxFailedAttempts()) {
            user.lockUntil(clock.instant().plus(properties.lockDuration()));
            user.resetFailedLogins();
        }
    }

    public void recordSuccess(User user)
    {
        user.resetFailedLogins();
        user.recordLogin(clock.instant());
    }
}
