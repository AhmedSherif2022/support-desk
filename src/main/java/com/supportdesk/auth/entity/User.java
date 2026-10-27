package com.supportdesk.auth.entity;
import com.supportdesk.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;



@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor()
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String username;


    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String fullName;


    @Size(max = 255)
    @NotNull
    @Column(name = "email", nullable = false)
    private String email;



    @ColumnDefault("false")
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;


    @Size(max = 20)
    @Column(name = "mobile_number", length = 20, nullable = false)
    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;


    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;


    private Instant lockedUntil;

    private Instant lastLoginAt;


    @Column(nullable = false)
    private int tokenVersion = 0;

     @Builder
    public User(String username, String passwordHash, String fullName, String email,String mobileNumber,Role role)
    {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email.trim().toLowerCase();
        this.mobileNumber=mobileNumber;
        this.role = role;
    }

    public void markVerified() {
        this.isVerified = true;
    }


    public void revokeTokens()
    {
        this.tokenVersion++;
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



