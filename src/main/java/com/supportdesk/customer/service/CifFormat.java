package com.supportdesk.customer.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agents may type a CIF with or without its prefix: "100245", "cif100245", "CIF 100245" and "CIF-100245"
 * all mean the stored CIF "CIF-100245". Anything that is not a CIF shape is returned trimmed and upper-cased,
 * so lookups still fail cleanly with "customer not found".
 */
public final class CifFormat
{
    public static final String PREFIX = "CIF-";

    // Optional "CIF" (any case), optional space or dash, then the digits.
    private static final Pattern CIF = Pattern.compile("^(?:CIF\\s*-?\\s*)?(\\d+)$", Pattern.CASE_INSENSITIVE);

    private CifFormat()
    {
    }

    /** The stored form of a typed CIF, e.g. "100245" -> "CIF-100245". Null becomes "". */
    public static String normalize(String input)
    {
        if (input == null) {
            return "";
        }
        String text = input.trim();
        Matcher m = CIF.matcher(text);
        return m.matches() ? PREFIX + m.group(1) : text.toUpperCase(Locale.ROOT);
    }
}
