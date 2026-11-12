package com.supportdesk.auth.service;
import com.supportdesk.auth.config.AuthProperties;
import com.supportdesk.auth.exception.PasswordNotHashedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The browser sends SHA-256(password) as hex, never the password itself, and stored hashes are BCrypt of that digest.
 * The local profile also takes the plain password (Swagger, Postman) and hashes it here.
 */
@Component
@RequiredArgsConstructor
public class PasswordDigest
{
    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-fA-F]{64}");

    private final AuthProperties properties;

    /** The digest to check against the stored BCrypt hash. */
    public String fromSubmitted(String submitted)
    {
        if (SHA256_HEX.matcher(submitted).matches()) {
            return submitted.toLowerCase(Locale.ROOT);
        }
        if (properties.allowPlainPassword()) {
            return sha256Hex(submitted);
        }
        throw new PasswordNotHashedException();                       // 400
    }

    public static String sha256Hex(String text)
    {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every JVM has SHA-256", e);
        }
    }
}
