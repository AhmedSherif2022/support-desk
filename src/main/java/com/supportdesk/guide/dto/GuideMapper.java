package com.supportdesk.guide.dto;

import com.supportdesk.guide.dto.GuideDtos.AnsweredStepDto;
import com.supportdesk.guide.dto.GuideDtos.CurrentStepDto;
import com.supportdesk.guide.dto.GuideDtos.GuideRunDto;
import com.supportdesk.guide.dto.GuideDtos.HintDto;
import com.supportdesk.guide.dto.GuideDtos.OptionDto;
import com.supportdesk.guide.dto.GuideDtos.OutcomeDto;
import com.supportdesk.guide.dto.GuideDtos.RecommendedGuideDto;
import com.supportdesk.guide.entity.Guide;
import com.supportdesk.guide.entity.GuideRun;
import com.supportdesk.guide.entity.GuideTree;
import com.supportdesk.guide.entity.GuideTree.AtOutcome;
import com.supportdesk.guide.entity.GuideTree.AtStep;
import com.supportdesk.guide.entity.GuideTree.Taken;
import com.supportdesk.guide.entity.GuideTree.Walk;
import com.supportdesk.guide.entity.GuideVersion;
import com.supportdesk.ticket.entity.Ticket;

import java.util.List;
import java.util.stream.IntStream;

public final class  GuideMapper
{
    private GuideMapper()
    {
    }

    public static RecommendedGuideDto toDto(GuideMatch match, Ticket ticket)
    {
        Guide guide = match.version().getGuide();
        String label = switch (match.matchedBy()) {
            case ERROR_CODE -> "Matches error " + ticket.customerErrorCode();
            case CATEGORY -> "Matches category " + ticket.getCategory().getName();
        };
        return new RecommendedGuideDto(guide.getId(), guide.getTitle(), match.version().getVersionNumber(),
                match.matchedBy(), label);
    }

    public static GuideRunDto toDto(GuideRun run)
    {
        GuideVersion version = run.getPinnedVersion();
        Walk walk = run.walk();
        List<Taken> taken = walk.taken();
        List<AnsweredStepDto> path = IntStream.range(0, taken.size())
                .mapToObj(i -> new AnsweredStepDto(i + 1, taken.get(i).stepKey(), taken.get(i).step().question(),
                        taken.get(i).answer().key(), taken.get(i).answer().label()))
                .toList();

        CurrentStepDto current = walk.position() instanceof AtStep at
                ? currentStep(version.getTree(), at, taken.size() + 1, run.getTicket().customerErrorCode()) : null;
        OutcomeDto outcome = walk.position() instanceof AtOutcome(String key, GuideTree.Outcome outcome1)
                ? new OutcomeDto(key, outcome1.type(), outcome1.result(),
                        outcome1.customerScript())
                : null;

        return new GuideRunDto(run.getId(), run.getTicket().displayNumber(), version.getGuide().getId(),
                version.getGuide().getTitle(), version.getVersionNumber(), run.status(), path, current, outcome,
                run.getStartedAt(), run.getStartedBy().getFullName());
    }

    private static CurrentStepDto currentStep(GuideTree tree, AtStep at, int stepNumber, String customerErrorCode)
    {
        List<OptionDto> options = at.step().answers().stream()
                .map(answer -> new OptionDto(answer.key(), answer.label()))
                .toList();
        HintDto hint = at.step().hintFor(customerErrorCode)
                .map(found -> new HintDto(found.answerKey(), found.text()))
                .orElse(null);
        return new CurrentStepDto(at.key(), at.step().question(), options, hint, stepNumber,
                stepNumber - 1 + tree.longestPathFrom(at.key()));
    }
}
