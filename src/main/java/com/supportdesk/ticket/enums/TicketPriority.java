package com.supportdesk.ticket.enums;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "How urgent the ticket is. It sets the SLA target (defaults): CRITICAL 2 hours, HIGH 8 hours, MEDIUM 24 hours, LOW 72 hours.")
public enum TicketPriority
{
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}