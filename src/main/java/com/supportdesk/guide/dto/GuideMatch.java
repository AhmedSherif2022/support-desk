package com.supportdesk.guide.dto;

import com.supportdesk.guide.entity.GuideVersion;
import com.supportdesk.guide.enums.GuideMatchType;

public record GuideMatch(GuideVersion version, GuideMatchType matchedBy)
{
}
