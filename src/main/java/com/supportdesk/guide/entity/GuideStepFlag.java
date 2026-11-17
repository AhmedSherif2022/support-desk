package com.supportdesk.guide.entity;

import com.supportdesk.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** E4-S5: an agent's report that one step of a finished run was wrong or unclear. Never edited. */
@Entity
@Table(name = "guide_step_flags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideStepFlag
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private GuideRun run;

    @Column(nullable = false, length = 60)
    private String stepKey;

    @Column(nullable = false, length = 1000)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flagged_by_id")
    private User flaggedBy;

    @Column(nullable = false)
    private Instant flaggedAt;

    public GuideStepFlag(GuideRun run, String stepKey, String comment, User flaggedBy, Instant flaggedAt)
    {
        this.run = run;
        this.stepKey = stepKey;
        this.comment = comment;
        this.flaggedBy = flaggedBy;
        this.flaggedAt = flaggedAt;
    }
}
