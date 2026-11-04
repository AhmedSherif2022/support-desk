package com.supportdesk.guide.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportdesk.guide.entity.GuideTree.Answer;
import com.supportdesk.guide.entity.GuideTree.AtOutcome;
import com.supportdesk.guide.entity.GuideTree.AtStep;
import com.supportdesk.guide.entity.GuideTree.Hint;
import com.supportdesk.guide.entity.GuideTree.Outcome;
import com.supportdesk.guide.entity.GuideTree.Step;
import com.supportdesk.guide.enums.OutcomeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** E4-S2 route rules on the tree itself, without Spring or a database. */
class GuideTreeTest
{
    private static final Outcome FIXED = new Outcome(OutcomeType.RESOLVED, "Fixed", "Please try again.");

    private static final String LOGIN_GUIDE = """
            {"start": "app-version",
             "steps": {
               "app-version":  {"question": "On 5.2 or later?",
                                "answers": [{"key": "yes", "label": "Yes", "next": "other-device"},
                                            {"key": "no",  "label": "No",  "outcome": "update"}]},
               "other-device": {"question": "Works on another device?",
                                "answers": [{"key": "yes", "label": "Yes", "outcome": "update"},
                                            {"key": "no",  "label": "No",  "next": "status-page"}],
                                "hints": [{"errorCode": "ERR_AUTH_503", "answerKey": "no", "text": "Fails everywhere."}]},
               "status-page":  {"question": "Incident on the status page?",
                                "answers": [{"key": "yes", "label": "Yes", "outcome": "wait"},
                                            {"key": "no",  "label": "No",  "outcome": "escalate"}]}},
             "outcomes": {
               "update":   {"type": "RESOLVED",  "result": "Updated", "customerScript": "Update the app."},
               "wait":     {"type": "FOLLOW_UP", "result": "Waiting", "customerScript": "We will call you."},
               "escalate": {"type": "ESCALATE",  "result": "Escalated", "customerScript": "Engineers are on it."}}}
            """;

    @Test
    void answersFollowThePublishedRouteToAnOutcome() throws Exception
    {
        GuideTree tree = new ObjectMapper().readValue(LOGIN_GUIDE, GuideTree.class);

        assertThat(tree.walk(List.of()).position()).isInstanceOfSatisfying(AtStep.class,
                at -> assertThat(at.key()).isEqualTo("app-version"));
        assertThat(tree.walk(List.of("yes", "no")).position()).isInstanceOfSatisfying(AtStep.class,
                at -> assertThat(at.key()).isEqualTo("status-page"));

        GuideTree.Walk done = tree.walk(List.of("yes", "no", "no"));
        assertThat(done.position()).isInstanceOfSatisfying(AtOutcome.class, at -> {
            assertThat(at.key()).isEqualTo("escalate");
            assertThat(at.outcome().type()).isEqualTo(OutcomeType.ESCALATE);
        });
        assertThat(done.taken()).extracting(GuideTree.Taken::stepKey)
                .containsExactly("app-version", "other-device", "status-page");
    }

    @Test
    void progressCountsTheLongestRouteLeft() throws Exception
    {
        GuideTree tree = new ObjectMapper().readValue(LOGIN_GUIDE, GuideTree.class);

        assertThat(tree.longestPathFrom("app-version")).isEqualTo(3);
        assertThat(tree.longestPathFrom("status-page")).isEqualTo(1);
    }

    @Test
    void aHintIsOfferedOnlyForItsErrorCode() throws Exception
    {
        Step step = new ObjectMapper().readValue(LOGIN_GUIDE, GuideTree.class).steps().get("other-device");

        assertThat(step.hintFor("ERR_AUTH_503")).map(Hint::answerKey).contains("no");
        assertThat(step.hintFor("PAY_TIMEOUT")).isEmpty();
        assertThat(step.hintFor(null)).isEmpty();
    }

    @Test
    void aTreeAnAgentCouldGetStuckInIsRejected()
    {
        assertThatThrownBy(() -> tree("missing", Map.of("a", step(new Answer("x", "X", null, "fixed")))))
                .hasMessageContaining("start step");
        assertThatThrownBy(() -> tree("a", Map.of("a", step(new Answer("x", "X", "nowhere", null)))))
                .hasMessageContaining("missing step");
        assertThatThrownBy(() -> tree("a", Map.of("a", step(new Answer("x", "X", null, "nothing")))))
                .hasMessageContaining("missing outcome");
        assertThatThrownBy(() -> tree("a", Map.of(
                "a", step(new Answer("x", "X", "b", null)),
                "b", step(new Answer("x", "X", "a", null)))))
                .hasMessageContaining("loops");
        assertThatThrownBy(() -> new Answer("x", "X", "b", "fixed")).hasMessageContaining("exactly one");
        assertThatThrownBy(() -> new Answer("x", "X", null, null)).hasMessageContaining("exactly one");
        assertThatThrownBy(() -> new Step("Q?", List.of(), null)).hasMessageContaining("no answers");
        assertThatThrownBy(() -> new Step("Q?", List.of(new Answer("x", "X", null, "fixed")),
                List.of(new Hint("E1", "y", "Try y")))).hasMessageContaining("missing answer");
    }

    private static GuideTree tree(String start, Map<String, Step> steps)
    {
        return new GuideTree(start, steps, Map.of("fixed", FIXED));
    }

    private static Step step(Answer answer)
    {
        return new Step("Question?", List.of(answer), null);
    }
}
