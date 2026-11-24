package com.supportdesk.health.service;
import com.supportdesk.customer.enums.Platform;

import java.time.Instant;
import java.util.List;

/** Read-only facts the panel classifies. Service names are already upper case. */
public interface ServiceHealthSignals
{
    record IncidentHit(Platform platform, String service, long incidentId) {}

    record CustomerCount(Platform platform, String service, int distinctCustomers) {}

    record NoticeHit(Platform platform, String service, long ticketNumber, boolean critical, boolean slaBreached) {}

    List<IncidentHit> openIncidentsWithOpenTickets();

    List<CustomerCount> distinctCustomersSince(Instant from);

    List<NoticeHit> openAutoTicketNotices();
}
