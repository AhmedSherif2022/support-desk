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
            Long guideId,
            String guideTitle,
            @Schema(description = "The version this run is pinned to (E4-S4).", example = "2") int version,
            GuideRunStatus status,
            @Schema(description = "Answers so far, first to last.") List<AnsweredStepDto> path,
            @Schema(description = "Null once an outcome is reached.", nullable = true) CurrentStepDto currentStep,
            @Schema(description = "Set once the answers lead to an outcome.", nullable = true) OutcomeDto outcome,
            Instant startedAt,
            String startedBy)
    {
    }

    public record AnsweredStepDto(
            @Schema(description = "1-based. DELETE .../answers/{position} goes back to this step.", example = "1") int position,
            String stepKey,
            String question,
            String answerKey,
            String answerLabel)
    {
    }

    public record CurrentStepDto(
            String stepKey,
            String question,
            List<OptionDto> options,
            @Schema(description = "Set when the step has a hint for the ticket's error code.", nullable = true) HintDto hint,
            @Schema(description = "This step's number.", example = "2") int stepNumber,
            @Schema(description = "The most steps this run can take from here.", example = "3") int maxSteps)
    {
    }

    public record OptionDto(String key, String label)
    {
    }

    public record HintDto(
            @Schema(description = "The answer the customer's logs point to.", example = "no") String answerKey,
            String text)
    {
    }

    public record OutcomeDto(
            String key,
            OutcomeType type,
            String result,
            @Schema(description = "What to say to the customer.") String customerScript)
    {
    }

    public record CompleteRunRequest(
            @Schema(example = "Customer updated the app and logged in.")
            @Size(max = 2000, message = "Notes can be at most 2000 characters.") String notes)
    {
    }

    public record CompletedRunDto(
            GuideRunDto run,
            @Schema(example = "RESOLVED") OutcomeType outcomeType,
            @Schema(description = "The ticket's status after the outcome.", example = "RESOLVED")
            com.supportdesk.ticket.enums.TicketStatus ticketStatus,
            @Schema(description = "Logged to Customer contact as \"Told customer: …\".") String customerScript)
    {
    }
}
