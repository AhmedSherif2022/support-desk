package com.supportdesk.auth.entity;
import com.supportdesk.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;



@Entity
@Table(name = "users")
@Getter

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String username;


    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Setter

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;


    private Instant lockedUntil;

    private Instant lastLoginAt;


    public User(String username, String passwordHash, String fullName, Role role)
    {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }


    public boolean isLocked(Instant now)
    {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }


    public void registerFailedLogin()
    {
        this.failedLoginAttempts++;
    }


    public void resetFailedLogins()
    {
        this.failedLoginAttempts = 0;
    }


    public void lockUntil(Instant until)
    {
        this.lockedUntil = until;
    }


    public void recordLogin(Instant when)
    {
        this.lastLoginAt = when;
        this.lockedUntil = null;
    }
}

