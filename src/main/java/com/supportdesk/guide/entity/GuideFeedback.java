package com.supportdesk.guide.entity;

import com.supportdesk.auth.entity.User;
import com.supportdesk.common.entity.BaseEntity;
import com.supportdesk.guide.enums.GuideFeedbackStatus;
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

/**
 * E4-S5: an agent's report that a step of a finished run was wrong or unclear. It points at the run and at the run's
 * pinned version, so the admin sees the exact question the agent saw even after a newer version goes live.
 */
@Entity
@Table(name = "guide_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideFeedback extends BaseEntity
{
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", updatable = false)
    private GuideRun run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_version_id", updatable = false)
    private GuideVersion guideVersion;

    @Column(nullable = false, updatable = false, length = 60)
    private String stepKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", updatable = false)
    private User agent;

    @Column(nullable = false, length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private GuideFeedbackStatus status;

    public GuideFeedback(GuideRun run, String stepKey, User agent, String comment)
    {
        this.run = run;
        this.guideVersion = run.getPinnedVersion();
        this.stepKey = stepKey;
        this.agent = agent;
        this.comment = comment.trim();
        this.status = GuideFeedbackStatus.OPEN;
    }

    /** False when it was already done, so a second click records nothing. */
    public boolean markDone()
    {
        if (status == GuideFeedbackStatus.RESOLVED) {
            return false;
        }
        status = GuideFeedbackStatus.RESOLVED;
        return true;
    }
}
