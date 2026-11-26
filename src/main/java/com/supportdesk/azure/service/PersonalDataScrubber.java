package com.supportdesk.azure.service;

import java.util.regex.Pattern;

/**
 * E5-S1: free text typed by agents (ticket title, description, escalation reason) may contain personal data. Before
 * anything goes to Azure DevOps, CIFs, 14-digit national IDs, Egyptian mobile numbers and the customer's name are
 * replaced with placeholders.
 */
public final class PersonalDataScrubber
{
    private static final Pattern CIF = Pattern.compile("(?i)\\bCIF\\s*-?\\s*\\d+\\b");
    private static final Pattern NATIONAL_ID = Pattern.compile("(?<!\\d)\\d{14}(?!\\d)");
    private static final Pattern MOBILE = Pattern.compile("(?<![\\d+])(?:\\+?20|0)1\\d{9}(?!\\d)");

    private PersonalDataScrubber()
    {
    }

    /** @param customerName the customer's full name, also removed (case-insensitive); may be null */
    public static String scrub(String text, String customerName)
    {
        if (text == null) {
            return null;
        }
        String out = NATIONAL_ID.matcher(text).replaceAll("[national ID removed]");
        out = MOBILE.matcher(out).replaceAll("[mobile removed]");
        out = CIF.matcher(out).replaceAll("[CIF removed]");
        if (customerName != null && !customerName.isBlank()) {
            out = Pattern.compile(Pattern.quote(customerName.trim()), Pattern.CASE_INSENSITIVE)
                    .matcher(out).replaceAll("[customer]");
        }
        return out;
    }
}
