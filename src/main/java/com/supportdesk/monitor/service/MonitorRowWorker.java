package com.supportdesk.monitor.service;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.repository.DetectedErrorEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Processes one detected error row per transaction: lock it (SKIP LOCKED), handle it, mark it.
 * A row that throws is rolled back and then set aside as FAILED in its own transaction,
 * so it is not picked first again on every run and does not block the rows behind it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorRowWorker
{
    private final DetectedErrorEventRepository eventRepository;
    private final ErrorEventProcessor processor;
    private final TransactionTemplate transactions;
    private final Clock clock;


    public Optional<EventOutcome> processNext()
    {
        AtomicReference<Long> lockedId = new AtomicReference<>();
        try {
            return transactions.execute(status -> {
                List<DetectedErrorEvent> rows = eventRepository.lockUnprocessed(1);
                if (rows.isEmpty()) {
                    return Optional.empty();
                }
                DetectedErrorEvent row = rows.get(0);
                lockedId.set(row.getId());
                return Optional.of(processor.process(row));
            });
        } catch (RuntimeException e) {
            Long id = lockedId.get();
            if (id == null) {
                throw e;                                    // could not even lock a row: let the run fail
            }
            log.error("Monitor job: error row {} failed and was set aside", id, e);
            transactions.executeWithoutResult(status -> eventRepository.markFailed(id, clock.instant()));
            return Optional.of(EventOutcome.FAILED);
        }
    }
}
