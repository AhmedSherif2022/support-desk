package com.supportdesk.guide.entity;

import com.supportdesk.guide.enums.OutcomeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A guide version's decision tree (guide_versions.tree). Construction rejects dead ends, missing steps and loops,
 * so callers follow routes without checks.
 */
public record GuideTree(String start, Map<String, Step> steps, Map<String, Outcome> outcomes)
{
    public GuideTree
    {
        steps = Map.copyOf(Objects.requireNonNull(steps, "steps"));
        outcomes = Map.copyOf(Objects.requireNonNull(outcomes, "outcomes"));
        require(steps.containsKey(start), "the start step '%s' does not exist", start);
        for (Map.Entry<String, Step> entry : steps.entrySet()) {
            for (Answer answer : entry.getValue().answers()) {
                require(answer.next() == null || steps.containsKey(answer.next()),
                        "step '%s' answer '%s' leads to a missing step '%s'", entry.getKey(), answer.key(), answer.next());
                require(answer.outcome() == null || outcomes.containsKey(answer.outcome()),
                        "step '%s' answer '%s' leads to a missing outcome '%s'", entry.getKey(), answer.key(), answer.outcome());
            }
        }
        Map<String, Integer> depths = new HashMap<>();
        for (String key : steps.keySet()) {
            depth(key, steps, new HashSet<>(), depths);
        }
    }

    public record Step(String question, List<Answer> answers, List<Hint> hints)
    {
        public Step
        {
            require(question != null && !question.isBlank(), "a step needs a question");
            answers = List.copyOf(Objects.requireNonNull(answers, "answers"));
            hints = hints == null ? List.of() : List.copyOf(hints);
            require(!answers.isEmpty(), "step '%s' has no answers", question);
            // The fields are not assigned until this constructor ends, so check the parameters, not answer().
            Set<String> keys = new HashSet<>();
            for (Answer answer : answers) {
                require(keys.add(answer.key()), "step '%s' repeats answer key '%s'", question, answer.key());
            }
            for (Hint hint : hints) {
                require(keys.contains(hint.answerKey()),
                        "step '%s' hints at a missing answer '%s'", question, hint.answerKey());
            }
        }

        public Optional<Answer> answer(String key)
        {
            return answers.stream().filter(answer -> answer.key().equals(key)).findFirst();
        }

        public Optional<Hint> hintFor(String errorCode)
        {
            return hints.stream().filter(hint -> hint.errorCode().equals(errorCode)).findFirst();
        }
    }

    public record Answer(String key, String label, String next, String outcome)
    {
        public Answer
        {
            require(key != null && !key.isBlank() && label != null && !label.isBlank(), "an answer needs a key and a label");
            require((next == null) != (outcome == null), "answer '%s' needs exactly one of next or outcome", key);
        }
    }

    public record Hint(String errorCode, String answerKey, String text)
    {
        public Hint
        {
            require(errorCode != null && answerKey != null && text != null, "a hint needs errorCode, answerKey and text");
        }
    }

    public record Outcome(OutcomeType type, String result, String customerScript)
    {
        public Outcome
        {
            require(type != null && result != null && customerScript != null,
                    "an outcome needs type, result and customerScript");
        }
    }

    public sealed interface Position permits AtStep, AtOutcome
    {
    }

    public record AtStep(String key, Step step) implements Position
    {
    }

    public record AtOutcome(String key, Outcome outcome) implements Position
    {
    }

    public record Taken(String stepKey, Step step, Answer answer)
    {
    }

    public record Walk(List<Taken> taken, Position position)
    {
    }

    public Walk walk(List<String> answerKeys)
    {
        List<Taken> taken = new ArrayList<>();
        Position position = new AtStep(start, steps.get(start));
        for (String answerKey : answerKeys) {
            if (!(position instanceof AtStep at)) {
                throw new IllegalStateException("Answer '" + answerKey + "' was given after the outcome");
            }
            Answer answer = at.step().answer(answerKey).orElseThrow(() -> new IllegalStateException(
                    "Step '" + at.key() + "' has no answer '" + answerKey + "'"));
            taken.add(new Taken(at.key(), at.step(), answer));
            position = answer.next() != null
                    ? new AtStep(answer.next(), steps.get(answer.next()))
                    : new AtOutcome(answer.outcome(), outcomes.get(answer.outcome()));
        }
        return new Walk(List.copyOf(taken), position);
    }

    /** Counts the step itself, so a last question returns 1. */
    public int longestPathFrom(String stepKey)
    {
        return depth(stepKey, steps, new HashSet<>(), new HashMap<>());
    }

    private static int depth(String key, Map<String, Step> steps, Set<String> onPath, Map<String, Integer> known)
    {
        Integer cached = known.get(key);
        if (cached != null) {
            return cached;
        }
        require(onPath.add(key), "step '%s' loops back to itself", key);
        int deepest = 0;
        for (Answer answer : steps.get(key).answers()) {
            if (answer.next() != null) {
                deepest = Math.max(deepest, depth(answer.next(), steps, onPath, known));
            }
        }
        onPath.remove(key);
        known.put(key, deepest + 1);
        return deepest + 1;
    }

    private static void require(boolean condition, String message, Object... args)
    {
        if (!condition) {
            throw new IllegalArgumentException("Guide tree: " + message.formatted(args));
        }
    }
}
