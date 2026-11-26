package com.supportdesk.azure.service;

import com.supportdesk.azure.config.AzureDevOpsProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * E5-S1: the customer as SRE sees them: a stable, masked token instead of CIF, national ID, mobile or name.
 * HMAC-SHA256 of the internal customer id with a secret that never leaves the portal, so SRE can tell that two work
 * items are the same customer, but cannot find out who the customer is.
 */
@Service
public class CustomerTokenService
{
    private final SecretKeySpec key;

    public CustomerTokenService(AzureDevOpsProperties props)
    {
        this.key = new SecretKeySpec(props.customerTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** e.g. "CUST-3F9A61C2D04B". */
    public String tokenFor(Long customerId)
    {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] digest = mac.doFinal(("customer:" + customerId).getBytes(StandardCharsets.UTF_8));
            return "CUST-" + HexFormat.of().withUpperCase().formatHex(digest, 0, 6);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot compute the customer token", e);
        }
    }
}
