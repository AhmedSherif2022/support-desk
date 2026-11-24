package com.supportdesk.health.enums;

/**
 * The bank services the home panel always lists, on MOBILE and on WEB.
 * Names match {@code detected_error_event.service} (AUTH, not the ticket category AUTHENTICATION).
 * A service with no errors still appears, as OPERATIONAL.
 */
public enum BankService
{
    AUTH,
    PAYMENTS,
    CARDS,
    TRANSFERS;

    public static boolean known(String service)
    {
        if (service == null) {
            return false;
        }
        for (BankService value : values()) {
            if (value.name().equals(service)) {
                return true;
            }
        }
        return false;
    }
}
