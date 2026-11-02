package com.supportdesk.customer.service;

/** Server-side masking (E2-S3): the full value only leaves the server through the audited reveal endpoint. */
public final class Masking
{
    private static final char DOT = '•';

    private Masking()
    {
    }

    /** "+201001234567" → "+20••••••4567": first 3 and last 4 visible. */
    public static String mobile(String mobile)
    {
        if (mobile == null || mobile.length() <= 7) {
            return "••••";
        }
        return mobile.substring(0, 3) + String.valueOf(DOT).repeat(mobile.length() - 7) + mobile.substring(mobile.length() - 4);
    }

    /** "29001011234567" → "••••••••••4567": last 4 visible. */
    public static String nationalId(String nationalId)
    {
        if (nationalId == null || nationalId.length() <= 4) {
            return "••••";
        }
        return String.valueOf(DOT).repeat(nationalId.length() - 4) + nationalId.substring(nationalId.length() - 4);
    }
}
