package com.supportdesk.incident.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One runtime setting the Guide Admin can tune, kept as text. Defaults are seeded by migration, never re-written. */
@Entity
@Table(name = "app_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppSetting
{
    @Id
    @Column(name = "setting_key", length = 80)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 255)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    public AppSetting(String key, String value, Instant now)
    {
        this.key = key;
        this.value = value;
        this.updatedAt = now;
    }

    public void change(String value, Instant now)
    {
        this.value = value;
        this.updatedAt = now;
    }
}
