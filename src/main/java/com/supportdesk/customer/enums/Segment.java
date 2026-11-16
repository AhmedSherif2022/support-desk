package com.supportdesk.customer.enums;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(enumAsRef = true, description = "The customer's segment: RETAIL, PREMIUM, VIP or BUSINESS.")
public enum Segment
{
    RETAIL,
    PREMIUM,
    VIP,
    BUSINESS
}
