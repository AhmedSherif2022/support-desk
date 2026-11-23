package com.supportdesk.audit.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The audit log filter accepts only the CIF number for customer records. */
class AuditQueryCifTest
{
    @Test
    void customerEntityIdCanBeJustTheNumber()
    {
        assertThat(AuditQueryService.customerId("CUSTOMER", "100245")).isEqualTo("CIF-100245");
        assertThat(AuditQueryService.customerId("customer", "CIF-100245")).isEqualTo("CIF-100245");
    }

    @Test
    void otherEntitiesAreLeftAlone()
    {
        assertThat(AuditQueryService.customerId("TICKET", "2001")).isEqualTo("2001");
        assertThat(AuditQueryService.customerId(null, "2001")).isEqualTo("2001");
        assertThat(AuditQueryService.customerId("CUSTOMER", null)).isNull();
    }
}
