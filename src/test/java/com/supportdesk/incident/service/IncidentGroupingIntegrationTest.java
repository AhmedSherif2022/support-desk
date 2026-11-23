package com.supportdesk.incident.service;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.repository.AuditLogRepository;
import com.supportdesk.incident.dto.IncidentDtos.UpdateIncidentSettingsRequest;
import com.supportdesk.incident.entity.Incident;
import com.supportdesk.incident.enums.IncidentStatus;
import com.supportdesk.incident.repository.IncidentRepository;
import com.supportdesk.incident.repository.IncidentTicketRepository;
import com.supportdesk.monitor.enums.JobTrigger;
import com.supportdesk.monitor.service.MonitorJob;
import com.supportdesk.ticket.enums.TicketPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E6-S3 against the real database: errors become automatic tickets through the monitor job, then group into one
 * incident. Each test uses its own error code so the seeded data and other tests never interfere. The scheduler is
 * off here so the test controls every run.
 */
@SpringBootTest(properties = "supportdesk.monitor.enabled=false")
class IncidentGroupingIntegrationTest
{
    @Autowired private MonitorJob monitorJob;
    @Autowired private IncidentGroupingService groupingService;
    @Autowired private IncidentSettingsService settingsService;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentTicketRepository incidentTicketRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JdbcTemplate jdbc;

    private String code;

    @BeforeEach
    void ownErrorRule()
    {
        code = "E6T_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        jdbc.update("INSERT INTO error_rule (error_code, category_id, issue, priority, auto_escalate, active, created_at, updated_at) "
                + "SELECT ?, id, 'E6 test issue', 'HIGH', FALSE, TRUE, now(), now() FROM ticket_categories WHERE code = 'LOGIN_FAILED'", code);
        settingsService.update(new UpdateIncidentSettingsRequest(5, 15));
    }

    @AfterEach
    void defaultsBack()
    {
        settingsService.update(new UpdateIncidentSettingsRequest(5, 15));
    }


    @Test
    void fiveCustomersBecomeOneIncidentWithCriticalTicketsAndAnAuditRecord()
    {
        for (int i = 1; i <= 5; i++) {
            error(newCustomer(), "MOBILE", 10 - i);
        }

        var run = monitorJob.run(JobTrigger.MANUAL);

        assertThat(run.getCreated()).isEqualTo(5);
        assertThat(run.getGrouped()).isEqualTo(5);
        Incident incident = openIncident();
        assertThat(incident.getAffectedCustomerCount()).isEqualTo(5);
        assertThat(incidentTicketRepository.countByIncidentId(incident.getId())).isEqualTo(5);
        assertThat(priorities()).containsOnly(TicketPriority.CRITICAL.name());
        assertThat(jdbc.queryForList("SELECT outcome FROM detected_error_event WHERE error_code = ?", String.class, code))
                .containsOnly("GROUPED");

        List<AuditLog> audits = auditLogRepository.findByEntityAndEntityIdOrderByCreatedAtDescIdDesc(
                IncidentGroupingService.ENTITY, String.valueOf(incident.getId()));
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getAction()).isEqualTo(AuditAction.INCIDENT_CREATED);
        assertThat(audits.get(0).getAfterState()).contains("\"errorCode\":\"" + code + "\"", "\"affectedCustomerCount\":5",
                "ticketNumbers").doesNotContain("nationalId", "mobile");
    }

    @Test
    void fourCustomersStaySeparateTickets()
    {
        for (int i = 1; i <= 4; i++) {
            error(newCustomer(), "MOBILE", 10 - i);
        }

        var run = monitorJob.run(JobTrigger.MANUAL);

        assertThat(run.getCreated()).isEqualTo(4);
        assertThat(run.getGrouped()).isZero();
        assertThat(incidentCount()).isZero();
        assertThat(priorities()).containsOnly(TicketPriority.HIGH.name());
    }

    @Test
    void reprocessingTheSameFailuresNeverMakesASecondIncidentOrASecondLink()
    {
        for (int i = 1; i <= 5; i++) {
            error(newCustomer(), "MOBILE", 10 - i);
        }
        monitorJob.run(JobTrigger.MANUAL);

        assertThat(groupingService.groupRecentFailures()).isZero();
        assertThat(monitorJob.run(JobTrigger.MANUAL).getGrouped()).isZero();

        assertThat(incidentCount()).isEqualTo(1);
        assertThat(incidentTicketRepository.countByIncidentId(openIncident().getId())).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM incident_ticket it JOIN tickets t ON t.id = it.ticket_id "
                + "WHERE t.error_code = ?", Long.class, code)).isEqualTo(5);
    }

    @Test
    void aLaterErrorOfTheSameOutageJoinsTheOpenIncidentAndANewCustomerIsCounted()
    {
        for (int i = 1; i <= 5; i++) {
            error(newCustomer(), "MOBILE", 12 - i);
        }
        monitorJob.run(JobTrigger.MANUAL);
        Incident incident = openIncident();

        error(newCustomer(), "MOBILE", 1);                              // a sixth customer, 6 minutes later
        var run = monitorJob.run(JobTrigger.MANUAL);

        assertThat(run.getCreated()).isEqualTo(1);
        assertThat(run.getGrouped()).isEqualTo(1);
        assertThat(incidentCount()).isEqualTo(1);
        Incident same = openIncident();
        assertThat(same.getId()).isEqualTo(incident.getId());
        assertThat(same.getAffectedCustomerCount()).isEqualTo(6);
        assertThat(incidentTicketRepository.countByIncidentId(same.getId())).isEqualTo(6);
        assertThat(priorities()).hasSize(6).containsOnly(TicketPriority.CRITICAL.name());
    }

    @Test
    void aSecondWaveAnHourLaterJoinsTheStillOpenIncident()
    {
        for (int i = 1; i <= 5; i++) {
            error(newCustomer(), "MOBILE", 10 - i);
        }
        monitorJob.run(JobTrigger.MANUAL);
        Incident incident = openIncident();
        // The first wave ended an hour ago: nothing new chains to it by time.
        jdbc.update("UPDATE incident SET first_occurred_at = now() - interval '70 minutes', "
                + "last_occurred_at = now() - interval '60 minutes' WHERE id = ?", incident.getId());

        for (int i = 1; i <= 5; i++) {
            error(newCustomer(), "MOBILE", 6 - i);                          // a new wave, 5 .. 1 minutes ago
        }
        var run = monitorJob.run(JobTrigger.MANUAL);

        assertThat(run.getGrouped()).isEqualTo(5);
        assertThat(incidentCount()).isEqualTo(1);
        Incident same = openIncident();
        assertThat(same.getId()).isEqualTo(incident.getId());
        assertThat(incidentTicketRepository.countByIncidentId(same.getId())).isEqualTo(10);
        assertThat(same.getAffectedCustomerCount()).isEqualTo(10);
        assertThat(auditLogRepository.findByEntityAndEntityIdOrderByCreatedAtDescIdDesc(
                IncidentGroupingService.ENTITY, String.valueOf(same.getId())))
                .extracting(AuditLog::getAction)
                .containsExactly(AuditAction.INCIDENT_TICKETS_LINKED, AuditAction.INCIDENT_CREATED);   // newest first
    }

    @Test
    void aRowTheMonitorHandlesLateStillJoinsTheRowsHandledEarlier()
    {
        for (int i = 1; i <= 4; i++) {
            error(newCustomer(), "MOBILE", 46 - i);                         // 45 .. 42 minutes ago
        }
        assertThat(monitorJob.run(JobTrigger.MANUAL).getGrouped()).isZero();  // 4 customers: below the threshold
        // Those four were handled long ago, so they are outside the two-window bound on when rows were handled.
        jdbc.update("UPDATE detected_error_event SET processed_at = now() - interval '2 hours' "
                + "WHERE error_code = ? AND processed_at IS NOT NULL", code);

        error(newCustomer(), "MOBILE", 41);                              // occurred 41 minutes ago, handled only now
        var run = monitorJob.run(JobTrigger.MANUAL);

        assertThat(run.getGrouped()).isEqualTo(5);
        assertThat(incidentCount()).isEqualTo(1);
    }

    @Test
    void thresholdsComeFromTheDatabaseAndSurviveARestart()
    {
        settingsService.update(new UpdateIncidentSettingsRequest(3, 30));

        // "restart": a fresh read goes to the table, not to anything cached in the service
        assertThat(jdbc.queryForObject("SELECT setting_value FROM app_settings WHERE setting_key = ?", String.class,
                IncidentSettingsService.MIN_CUSTOMERS_KEY)).isEqualTo("3");
        assertThat(settingsService.current().minAffectedCustomers()).isEqualTo(3);
        assertThat(settingsService.current().groupingWindowMinutes()).isEqualTo(30);

        for (int i = 1; i <= 3; i++) {
            error(newCustomer(), "WEB", 25 - i * 8);                    // 17, 9 and 1 minutes ago: inside 30, outside 15
        }
        var run = monitorJob.run(JobTrigger.MANUAL);

        // the run may also group leftovers of other tests under the lower threshold, so look at this code only
        assertThat(run.getGrouped()).isGreaterThanOrEqualTo(3);
        assertThat(incidentCount()).isEqualTo(1);
        assertThat(incidentTicketRepository.countByIncidentId(openIncident().getId())).isEqualTo(3);
        assertThat(openIncident().getAffectedCustomerCount()).isEqualTo(3);
    }

    @Test
    void everyThresholdChangeIsAuditedWithTheOldAndNewValue()
    {
        int windowAuditsBefore = auditLogRepository.findByEntityAndEntityIdOrderByCreatedAtDescIdDesc(
                IncidentSettingsService.ENTITY, IncidentSettingsService.WINDOW_MINUTES_KEY).size();

        settingsService.update(new UpdateIncidentSettingsRequest(7, 15));

        List<AuditLog> audits = auditLogRepository.findByEntityAndEntityIdOrderByCreatedAtDescIdDesc(
                IncidentSettingsService.ENTITY, IncidentSettingsService.MIN_CUSTOMERS_KEY);
        assertThat(audits).isNotEmpty();
        AuditLog latest = audits.get(0);
        assertThat(latest.getAction()).isEqualTo(AuditAction.SETTING_CHANGED);
        assertThat(latest.getBeforeState()).contains("\"value\":\"5\"");
        assertThat(latest.getAfterState()).contains("\"value\":\"7\"");
        // the window did not change, so nothing was recorded for it by this update
        assertThat(auditLogRepository.findByEntityAndEntityIdOrderByCreatedAtDescIdDesc(
                IncidentSettingsService.ENTITY, IncidentSettingsService.WINDOW_MINUTES_KEY)).hasSize(windowAuditsBefore);
    }

    @Test
    void concurrentGroupingRunsOpenExactlyOneIncident() throws Exception
    {
        settingsService.update(new UpdateIncidentSettingsRequest(100, 15));   // tickets first, no incident yet
        for (int i = 1; i <= 5; i++) {
            error(newCustomer(), "MOBILE", 10 - i);
        }
        monitorJob.run(JobTrigger.MANUAL);
        assertThat(incidentCount()).isZero();
        settingsService.update(new UpdateIncidentSettingsRequest(5, 15));

        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                go.await(5, TimeUnit.SECONDS);
                return groupingService.groupRecentFailures();
            }));
        }
        go.countDown();
        int linked = 0;
        for (Future<Integer> result : results) {
            linked += result.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(linked).isEqualTo(5);                                 // one run did the work, the others found nothing
        assertThat(incidentCount()).isEqualTo(1);
        assertThat(incidentTicketRepository.countByIncidentId(openIncident().getId())).isEqualTo(5);
        assertThat(auditLogRepository.findByEntityAndEntityIdOrderByCreatedAtDescIdDesc(
                IncidentGroupingService.ENTITY, String.valueOf(openIncident().getId()))).hasSize(1);
    }


    // ---- helpers ----

    private long newCustomer()
    {
        String cif = "CIF-E6" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
        String mobile = String.format("+2098%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        String nid = String.format("%014d", ThreadLocalRandom.current().nextLong(10_000_000_000_000L, 99_999_999_999_999L));
        jdbc.update("INSERT INTO customers (cif, full_name, national_id, mobile, date_of_birth, segment, preferred_language, created_at, updated_at) "
                + "VALUES (?, 'E6 Test', ?, ?, '1990-01-01', 'RETAIL', 'EN', now(), now())", cif, nid, mobile);
        return jdbc.queryForObject("SELECT id FROM customers WHERE cif = ?", Long.class, cif);
    }

    private void error(long customerId, String platform, int minutesAgo)
    {
        jdbc.update("INSERT INTO detected_error_event (customer_id, error_code, platform, service, message, correlation_id, occurred_at) "
                + "VALUES (?, ?, ?, 'AUTH', 'E6 test', ?, now() - make_interval(mins => ?))",
                customerId, code, platform, "E6T-" + UUID.randomUUID(), minutesAgo);
    }

    private Incident openIncident()
    {
        return incidentRepository.findAll().stream()
                .filter(i -> i.getErrorCode().equals(code) && i.getStatus() == IncidentStatus.OPEN)
                .findFirst().orElseThrow();
    }

    private List<String> priorities()
    {
        return jdbc.queryForList("SELECT priority FROM tickets WHERE error_code = ?", String.class, code);
    }

    private long incidentCount()
    {
        return jdbc.queryForObject("SELECT COUNT(*) FROM incident WHERE error_code = ?", Long.class, code);
    }
}
