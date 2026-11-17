package com.supportdesk.guide.dto;

import com.supportdesk.guide.enums.GuideMatchType;
import com.supportdesk.guide.enums.GuideRunStatus;
import com.supportdesk.guide.enums.OutcomeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class GuideDtos
{
    private GuideDtos()
    {
    }

    public record RecommendedGuideDto(
            @Schema(example = "1") Long guideId,
            @Schema(example = "Login failed: server error") String title,
            @Schema(description = "The live version a run would start on.", example = "2") int liveVersion,
            @Schema(description = "ERROR_CODE matches come first.") GuideMatchType matchedBy,
            @Schema(example = "Matches error ERR_AUTH_503") String matchLabel)
    {
    }

    public record StartGuideRunRequest(
            @Schema(description = "Ticket number, without the #.", example = "2002")
            @NotNull(message = "Choose the ticket.") Long ticketNumber,
            @Schema(description = "A recommended guide. The run starts on its live version.", example = "1")
            @NotNull(message = "Choose a guide.") Long guideId)
    {
    }

    public record AnswerRequest(
            @Schema(description = "The step the agent answered: the run's current step.", example = "app-version")
            @NotBlank(message = "Say which step this answers.") String stepKey,
            @Schema(example = "yes")
            @NotBlank(message = "Choose an answer.") String answerKey)
    {
    }

    public record GuideRunDto(
            @Schema(example = "17") Long id,
            @Schema(example = "#2002") String ticketNumber,
            @Schema(example = "3") Long guideId,
            @Schema(example = "Login failed: server error") String guideTitle,
            @Schema(description = "The version this run is pinned to.", example = "2") int version,
            @Schema(example = "IN_PROGRESS") GuideRunStatus status,
            @Schema(description = "Answers so far, first to last.") List<AnsweredStepDto> path,
            @Schema(description = "Null once an outcome is reached or the run has ended.", nullable = true) CurrentStepDto currentStep,
            @Schema(description = "Set once the answers lead to an outcome. On an ABANDONED run it was reached but "
                    + "never applied.", nullable = true) OutcomeDto outcome,
            @Schema(example = "2026-11-16T10:35:00Z") Instant startedAt,
            @Schema(description = "Full name of the agent who started it.", example = "Mona") String startedBy,
            @Schema(description = "When the run was completed or abandoned; null while it is open.",
                    example = "2026-11-16T10:41:00Z", types = {"string", "null"}) Instant completedAt)
    {
    }

    public record AnsweredStepDto(
            @Schema(description = "1-based. DELETE .../answers/{position} goes back to this step.", example = "1") int position,
            @Schema(example = "app-version") String stepKey,
            @Schema(example = "Is the app on the latest version?") String question,
            @Schema(example = "yes") String answerKey,
            @Schema(example = "Yes") String answerLabel)
    {
    }

    public record CurrentStepDto(
            @Schema(description = "Send this as stepKey when answering.", example = "other-device") String stepKey,
            @Schema(example = "Does login work on another device?") String question,
            @Schema(description = "The answers to choose from; send one key as answerKey.") List<OptionDto> options,
            @Schema(description = "Set when the step has a hint for the ticket's error code.", nullable = true) HintDto hint,
            @Schema(description = "This step's number.", example = "2") int stepNumber,
            @Schema(description = "Steps on the longest path through this step, counting the ones already answered: "
                    + "the N in \"Step 2 of N\". It can drop as answers rule out longer paths.", example = "3") int maxSteps)
    {
    }

    public record OptionDto(@Schema(example = "no") String key, @Schema(example = "No") String label)
    {
    }

    public record HintDto(
            @Schema(description = "The answer the customer's logs point to.", example = "no") String answerKey,
            @Schema(example = "The customer's logs show ERR_AUTH_503 on mobile only.") String text)
    {
    }

    public record OutcomeDto(
            @Schema(example = "escalate-sre") String key,
            @Schema(example = "ESCALATE") OutcomeType type,
            @Schema(description = "What the agent found.", example = "Login fails everywhere with no known incident.") String result,
            @Schema(description = "What to say to the customer.") String customerScript)
    {
    }

    public record CompleteRunRequest(
            @Schema(description = "Optional agent notes, saved on the run (no endpoint returns them). Send {} when "
                    + "there are none.", example = "Customer updated the app and logged in.")
            @Size(max = 2000, message = "Notes can be at most 2000 characters.") String notes)
    {
    }

    public record CompletedRunDto(
            @Schema(description = "The run, now COMPLETED.") GuideRunDto run,
            @Schema(example = "RESOLVED") OutcomeType outcomeType,
            @Schema(description = "The ticket's status after the outcome.", example = "RESOLVED")
            com.supportdesk.ticket.enums.TicketStatus ticketStatus,
            @Schema(description = "Logged to Customer contact as \"Told customer: …\".") String customerScript)
    {
    }

    public record FlagStepRequest(
            @Schema(description = "A stepKey from the run's path, or the step an abandoned run stopped on.", example = "other-device")
            @NotBlank(message = "Say which step you are flagging.") String stepKey,
            @Schema(description = "What was wrong or unclear about the step.",
                    example = "The question does not say which device to try.")
            @NotBlank(message = "Write what was wrong or unclear.")
            @Size(max = 1000, message = "The comment can be at most 1000 characters.") String comment)
    {
    }

    public record StepFlagDto(
            @Schema(example = "5") Long id,
            @Schema(example = "17") Long guideRunId,
            @Schema(example = "3") Long guideId,
            @Schema(example = "Login failed: server error") String guideTitle,
            @Schema(description = "The version the run used, so the Guide Admin fixes the right one.", example = "2") int version,
            @Schema(example = "other-device") String stepKey,
            @Schema(example = "Can the customer log in on the web or on another device?") String question,
            @Schema(example = "The question does not say which device to try.") String comment,
            @Schema(description = "Full name of the agent who flagged it.", example = "Mona") String flaggedBy,
            @Schema(example = "2026-11-16T10:45:00Z") Instant flaggedAt)
    {
    }
}
