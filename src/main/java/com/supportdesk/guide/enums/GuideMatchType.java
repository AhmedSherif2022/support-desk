package com.supportdesk.guide.enums;
import io.swagger.v3.oas.annotations.media.Schema;

/** Why a guide was recommended. Declared in ranking order: an error-code match is preferred. */
@Schema(enumAsRef = true, description = "Why a guide was recommended. An error-code match ranks first.\n"
        + "- ERROR_CODE: the guide is for the error code the customer got\n"
        + "- CATEGORY: the guide is for the ticket's category")
public enum GuideMatchType
{
    ERROR_CODE,
    CATEGORY
}
