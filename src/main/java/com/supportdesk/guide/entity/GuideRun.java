package com.supportdesk.guide.entity;

import com.supportdesk.auth.entity.User;
import com.supportdesk.common.entity.BaseEntity;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.guide.entity.GuideTree.AtOutcome;
import com.supportdesk.guide.entity.GuideTree.AtStep;
import com.supportdesk.guide.entity.GuideTree.Walk;
import com.supportdesk.guide.enums.GuideRunStatus;
import com.supportdesk.ticket.entity.Ticket;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Only the answers are stored: the current step and the outcome are replayed from them against the pinned tree. */
@Entity
@Table(name = "troubleshooting_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideRun extends BaseEntity
{
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_version_id", updatable = false)
    private GuideVersion pinnedVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "started_by_id")
    private User startedBy;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<GuideRunAnswer> answers = new ArrayList<>();

    public GuideRun(Ticket ticket, GuideVersion pinnedVersion, User startedBy, Instant startedAt)
    {
        this.ticket = ticket;
        this.pinnedVersion = pinnedVersion;
        this.startedBy = startedBy;
        this.startedAt = startedAt;
    }

    public Walk walk()
    {
        Walk walk = pinnedVersion.getTree().walk(answers.stream().map(GuideRunAnswer::getAnswerKey).toList());
        for (int i = 0; i < answers.size(); i++) {
            if (!walk.taken().get(i).stepKey().equals(answers.get(i).getStepKey())) {
                throw new IllegalStateException("Guide run " + getId() + " answer " + (i + 1) + " is stored for step '"
                        + answers.get(i).getStepKey() + "' but replays onto '" + walk.taken().get(i).stepKey() + "'");
            }
        }
        return walk;
    }

    public GuideRunStatus status()
    {
        if (completedAt != null) {
            return GuideRunStatus.COMPLETED;
        }
        return walk().position() instanceof AtOutcome ? GuideRunStatus.OUTCOME_REACHED : GuideRunStatus.IN_PROGRESS;
    }

    public boolean isActive()
    {
        return completedAt == null;
    }

    public void answer(String stepKey, String answerKey, Instant now)
    {
        if (!(walk().position() instanceof AtStep at) || !at.key().equals(stepKey)) {
            throw new BusinessRuleException(ErrorCode.INVALID_GUIDE_ROUTE,
                    "This run is not on step '" + stepKey + "' any more. Reload it.");
        }
        if (at.step().answer(answerKey).isEmpty()) {
            throw new BusinessRuleException(ErrorCode.INVALID_GUIDE_ROUTE,
                    "'" + answerKey + "' is not an answer to step '" + stepKey + "'.");
        }
        answers.add(new GuideRunAnswer(this, answers.size() + 1, stepKey, answerKey, now));
    }

    public void clearFrom(int position)
    {
        answers.removeIf(answer -> answer.getPosition() >= position);
    }

    /** Ends a run its ticket has moved on from, so the ticket can start a guide again. */
    public void abandon(Instant now)
    {
        completedAt = now;
    }
}
