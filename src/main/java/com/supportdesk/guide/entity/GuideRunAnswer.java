package com.supportdesk.guide.entity;

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

@Entity
@Table(name = "troubleshooting_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideRunAnswer
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private GuideRun run;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 60)
    private String stepKey;

    @Column(nullable = false, length = 60)
    private String answerKey;

    @Column(nullable = false)
    private Instant answeredAt;

    GuideRunAnswer(GuideRun run, int position, String stepKey, String answerKey, Instant answeredAt)
    {
        this.run = run;
        this.position = position;
        this.stepKey = stepKey;
        this.answerKey = answerKey;
        this.answeredAt = answeredAt;
    }
}
