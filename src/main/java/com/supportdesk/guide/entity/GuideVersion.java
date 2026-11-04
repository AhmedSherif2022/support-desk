package com.supportdesk.guide.entity;

import com.supportdesk.common.entity.BaseEntity;
import com.supportdesk.guide.enums.GuideVersionState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "guide_versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideVersion extends BaseEntity
{
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id")
    private Guide guide;

    @Column(nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private GuideVersionState state;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private GuideTree tree;

    private Instant publishedAt;
}
