package com.supportdesk.monitor.dto;
import com.supportdesk.monitor.entity.MonitorJobRun;
import com.supportdesk.monitor.enums.JobTrigger;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public final class MonitorDtos
{
    private MonitorDtos()
    {
    }

    public record JobRunDto(
            @Schema(example = "12") Long id,
            @Schema(example = "SCHEDULE") JobTrigger trigger,
            @Schema(example = "2026-11-22T10:42:00Z") Instant startedAt,
            @Schema(description = "Null while the run is still going.", types = {"string", "null"}) Instant finishedAt,
            @Schema(example = "9") int rowsRead,
            @Schema(example = "7") int created,
            @Schema(example = "1") int duplicates,
            @Schema(example = "0") int grouped,
            @Schema(example = "1") int unknownCodes,
            @Schema(example = "0", description = "Rows that threw and were set aside as FAILED.") int failed,
            @Schema(description = "Why the run stopped; null when it finished normally.", types = {"string", "null"}) String error)
    {
        public static JobRunDto from(MonitorJobRun run)
        {
            return new JobRunDto(run.getId(), run.getTrigger(), run.getStartedAt(), run.getFinishedAt(),
                    run.getRowsRead(), run.getCreated(), run.getDuplicates(), run.getGrouped(),
                    run.getUnknownCodes(), run.getFailed(), run.getError());
        }
    }
}
