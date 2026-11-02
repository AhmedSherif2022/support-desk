package com.supportdesk.ticket.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(TicketProperties.class)
public class TicketConfig
{
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(
            name = "supportdesk.tickets.sla.scheduling-enabled",
            havingValue = "true",
            matchIfMissing = true)
    static class SchedulingConfig
    {
    }
}
