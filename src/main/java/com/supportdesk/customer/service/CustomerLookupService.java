package com.supportdesk.customer.service;

import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.exception.CustomerNotFoundException;
import com.supportdesk.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Finds a customer by CIF. What may be shown about them is decided by Customer360Service. */
@Service
@RequiredArgsConstructor
public class CustomerLookupService
{
    private final CustomerRepository customerRepository;


    @Transactional(readOnly = true)
    public Customer getByCif(String cif)
    {
        String clean = cif == null ? "" : cif.trim();
        return customerRepository.findByCifIgnoreCase(clean)
                .orElseThrow(() -> new CustomerNotFoundException(clean));
    }
}
