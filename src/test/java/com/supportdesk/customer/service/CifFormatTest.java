package com.supportdesk.customer.service;

import com.supportdesk.audit.service.AuditService;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.exception.CustomerNotFoundException;
import com.supportdesk.customer.repository.CustomerRepository;
import com.supportdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Agents can type only the CIF number (or the full CIF) in every customer lookup and in the search box. */
class CifFormatTest
{
    @ParameterizedTest
    @ValueSource(strings = {"100245", " 100245 ", "CIF-100245", "cif-100245", "CIF100245", "cif 100245", "CIF - 100245"})
    void everyWayOfTypingTheCifGivesTheStoredForm(String typed)
    {
        assertThat(CifFormat.normalize(typed)).isEqualTo("CIF-100245");
    }

    @Test
    void somethingThatIsNotACifIsOnlyTrimmedAndUpperCased()
    {
        assertThat(CifFormat.normalize(" abc-12x ")).isEqualTo("ABC-12X");
        assertThat(CifFormat.normalize("CIF-")).isEqualTo("CIF-");
        assertThat(CifFormat.normalize(null)).isEmpty();
    }

    @Test
    void lookupFindsTheCustomerFromTheNumberAlone()
    {
        CustomerRepository repository = mock(CustomerRepository.class);
        Customer customer = mock(Customer.class);
        when(repository.findByCifIgnoreCase("CIF-100245")).thenReturn(Optional.of(customer));

        assertThat(new CustomerLookupService(repository).getByCif("100245")).isSameAs(customer);
    }

    @Test
    void unknownNumberStillGivesCustomerNotFound()
    {
        CustomerRepository repository = mock(CustomerRepository.class);
        when(repository.findByCifIgnoreCase("CIF-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CustomerLookupService(repository).getByCif("999999"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("CIF-999999");
    }

    @Test
    void searchBoxLooksUpTheCifFromTheNumberAlone()
    {
        CustomerRepository repository = mock(CustomerRepository.class);
        when(repository.findByCifIgnoreCase("CIF-100245")).thenReturn(Optional.empty());
        CustomerSearchService search = new CustomerSearchService(repository, mock(TicketRepository.class),
                mock(SearchRateLimiter.class), mock(CurrentUserService.class), mock(AuditService.class));

        search.search("100245");

        verify(repository).findByCifIgnoreCase("CIF-100245");
    }
}
