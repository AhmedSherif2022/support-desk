package com.supportdesk.health.service;
import com.supportdesk.health.enums.ServiceHealthStatus;

/**
 * Turns the signals for one service and platform into a status. Notices (a critical or SLA-breached ticket)
 * are not an input: they are shown beside the status and never change it.
 */
public final class ServiceHealthClassifier
{
    private ServiceHealthClassifier()
    {
    }

    public static ServiceHealthStatus classify(boolean openIncidentWithOpenTicket, int distinctCustomers,
                                                int minCustomers, boolean dataStale)
    {
        if (openIncidentWithOpenTicket) {
            return ServiceHealthStatus.DOWN;
        }
        if (distinctCustomers >= minCustomers) {
            return ServiceHealthStatus.DEGRADED;
        }
        if (dataStale) {
            return ServiceHealthStatus.UNKNOWN;
        }
        return ServiceHealthStatus.OPERATIONAL;
    }
}
