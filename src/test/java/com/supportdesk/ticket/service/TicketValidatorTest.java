package com.supportdesk.ticket.service;

import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.entity.TicketPriority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** E3-S1: Transfers need a TRX- reference; a ticket needs a sub-category. */
class TicketValidatorTest
{
    private final TicketValidator validator = new TicketValidator();

    private final TicketCategory transfers = new TicketCategory("TRANSFERS", "Transfers", null,
            TicketPriority.HIGH, false, null, null, null);                                   // top level
    private final TicketCategory transferFailed = new TicketCategory("TRANSFER_FAILED", "Transfer Failed", transfers,
            TicketPriority.HIGH, true, "^TRX-\\d{4}-\\d{5}$", "Use the format TRX-2026-00981", "TRX_DEBIT_NO_CREDIT");

    @Test
    void missingReferenceIsRejected()
    {
        assertThatThrownBy(() -> validator.checkReference(transferFailed, ""))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Transaction reference must start with TRX-");
    }

    @Test
    void wrongFormatGetsTheHint()
    {
        assertThatThrownBy(() -> validator.checkReference(transferFailed, "TRX-12"))
                .hasMessage("Use the format TRX-2026-00981");
    }

    @Test
    void goodReferenceIsCleaned()
    {
        assertThat(validator.checkReference(transferFailed, " trx-2026-00981 ")).isEqualTo("TRX-2026-00981");
    }

    @Test
    void topLevelCategoryIsNotEnough()
    {
        assertThatThrownBy(() -> validator.checkCategory(transfers))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Choose a sub-category.");
    }
}
