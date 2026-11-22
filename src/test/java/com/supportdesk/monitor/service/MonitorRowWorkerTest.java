package com.supportdesk.monitor.service;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.repository.DetectedErrorEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MonitorRowWorkerTest
{
    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    private final DetectedErrorEventRepository repository = mock(DetectedErrorEventRepository.class);
    private final ErrorEventProcessor processor = mock(ErrorEventProcessor.class);
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private MonitorRowWorker worker;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp()
    {
        // Run the callbacks straight away, as a real transaction would.
        when(transactions.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(mock(TransactionStatus.class)));
        doAnswer(inv -> {
            ((Consumer<TransactionStatus>) inv.getArgument(0)).accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        worker = new MonitorRowWorker(repository, processor, transactions, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void nothingToProcessIsEmpty()
    {
        when(repository.lockUnprocessed(1)).thenReturn(List.of());

        assertThat(worker.processNext()).isEmpty();
    }

    @Test
    void processedRowReturnsItsOutcome()
    {
        DetectedErrorEvent row = row(7L);
        when(repository.lockUnprocessed(1)).thenReturn(List.of(row));
        when(processor.process(row)).thenReturn(EventOutcome.CREATED);

        assertThat(worker.processNext()).contains(EventOutcome.CREATED);
        verify(repository, never()).markFailed(any(), any());
    }

    @Test
    void rowThatThrowsIsSetAsideSoItDoesNotBlockTheQueue()
    {
        DetectedErrorEvent row = row(7L);
        when(repository.lockUnprocessed(1)).thenReturn(List.of(row));
        when(processor.process(row)).thenThrow(new IllegalStateException("boom"));

        assertThat(worker.processNext()).contains(EventOutcome.FAILED);
        verify(repository).markFailed(7L, NOW);
    }

    @Test
    void failureBeforeAnyRowIsLockedFailsTheRun()
    {
        when(repository.lockUnprocessed(1)).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> worker.processNext()).hasMessage("db down");
        verify(repository, never()).markFailed(any(), any());
    }

    private static DetectedErrorEvent row(Long id)
    {
        DetectedErrorEvent row = mock(DetectedErrorEvent.class);
        when(row.getId()).thenReturn(id);
        return row;
    }
}
