package com.supportdesk.health.service;
import com.supportdesk.health.enums.ServiceHealthStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceHealthClassifierTest
{
    @Test
    void anOpenIncidentWithAnOpenTicketIsDownEvenWhenTheMonitorIsStale()
    {
        assertThat(ServiceHealthClassifier.classify(true, 0, 5, true)).isEqualTo(ServiceHealthStatus.DOWN);
    }

    @Test
    void enoughDistinctCustomersIsDegradedAndDoesNotNeedAnIncident()
    {
        assertThat(ServiceHealthClassifier.classify(false, 5, 5, false)).isEqualTo(ServiceHealthStatus.DEGRADED);
    }

    @Test
    void oneCustomerBelowTheThresholdStaysOperational()
    {
        assertThat(ServiceHealthClassifier.classify(false, 4, 5, false)).isEqualTo(ServiceHealthStatus.OPERATIONAL);
    }

    @Test
    void aStaleMonitorTurnsAQuietRowUnknownInsteadOfOperational()
    {
        assertThat(ServiceHealthClassifier.classify(false, 0, 5, true)).isEqualTo(ServiceHealthStatus.UNKNOWN);
    }

    @Test
    void aStaleMonitorDoesNotHideARealDegradedRow()
    {
        assertThat(ServiceHealthClassifier.classify(false, 9, 5, true)).isEqualTo(ServiceHealthStatus.DEGRADED);
    }
}
