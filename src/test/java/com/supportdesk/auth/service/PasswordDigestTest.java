package com.supportdesk.auth.service;

import com.supportdesk.auth.config.AuthProperties;
import com.supportdesk.auth.exception.PasswordNotHashedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordDigestTest
{
    /** SHA-256 of the local seed password, as the browser sends it. */
    private static final String DIGEST = "a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea";

    @Test
    void sha256MatchesWhatTheBrowserSends()
    {
        assertThat(PasswordDigest.sha256Hex("Password123!")).isEqualTo(DIGEST);
    }

    @Test
    void aDigestIsUsedAsIsInAnyCase()
    {
        assertThat(digest(false).fromSubmitted(DIGEST.toUpperCase())).isEqualTo(DIGEST);
    }

    @Test
    void aPlainPasswordIsRejectedOutsideLocal()
    {
        assertThatThrownBy(() -> digest(false).fromSubmitted("Password123!"))
                .isInstanceOf(PasswordNotHashedException.class);
    }

    @Test
    void aPlainPasswordIsHashedWhenAllowed()
    {
        assertThat(digest(true).fromSubmitted("Password123!")).isEqualTo(DIGEST);
    }

    private static PasswordDigest digest(boolean allowPlain)
    {
        return new PasswordDigest(new AuthProperties("secret", Duration.ofMinutes(15), 5, Duration.ofMinutes(15),
                Duration.ofMinutes(15), Duration.ofMinutes(30), allowPlain));
    }
}
