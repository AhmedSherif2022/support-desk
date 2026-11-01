package com.supportdesk.ticket.service;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.ticket.entity.TicketCategory;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class TicketValidator
{
    private static final String TRX_PREFIX = "TRX-";


    public void checkCategory(TicketCategory category)
    {
        if (!category.isActive()) {
            throw new BusinessRuleException(ErrorCode.CATEGORY_INACTIVE);
        }
        if (!category.isSubCategory()) {
            throw new BusinessRuleException(ErrorCode.SUBCATEGORY_REQUIRED);
        }
    }


    public String checkReference(TicketCategory category, String reference)
    {
        String ref = reference == null ? "" : reference.trim().toUpperCase(Locale.ROOT);

        if (!category.isReferenceRequired()) {
            return ref.isEmpty() ? null : ref;                 // optional: keep it if typed
        }
        if (!ref.startsWith(TRX_PREFIX)) {
            throw new BusinessRuleException(ErrorCode.REFERENCE_REQUIRED);
        }
        String pattern = category.getReferencePattern();
        if (pattern != null && !Pattern.matches(pattern, ref)) {
            if (category.getReferenceHint() != null) {
                // the category's own hint, e.g. "Use the format TRX-2026-00981"
                throw new BusinessRuleException(ErrorCode.REFERENCE_FORMAT, category.getReferenceHint());
            }
            throw new BusinessRuleException(ErrorCode.REFERENCE_FORMAT);
        }
        return ref;
    }
}
