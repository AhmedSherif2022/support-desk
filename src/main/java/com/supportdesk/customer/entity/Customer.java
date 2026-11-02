package com.supportdesk.customer.entity;
import com.supportdesk.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseEntity
{

    @Column(nullable = false, unique = true, length = 30)
    private String cif;

    @Column(nullable = false, length = 150)
    private String fullName;


    @Column(nullable = false, unique = true, length = 20)
    private String nationalId;

    @Column(nullable = false, length = 20)
    private String mobile;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Segment segment;


    @Column(nullable = false, length = 2)
    private String preferredLanguage;

    public Customer(String cif, String fullName, String nationalId, String mobile,
                    LocalDate dateOfBirth, Segment segment, String preferredLanguage)
    {
        this.cif = cif;
        this.fullName = fullName;
        this.nationalId = nationalId;
        this.mobile = mobile;
        this.dateOfBirth = dateOfBirth;
        this.segment = segment;
        this.preferredLanguage = preferredLanguage;
    }
}
