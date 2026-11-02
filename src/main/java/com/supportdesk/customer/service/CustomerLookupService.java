package com.supportdesk.customer.service;
import com.supportdesk.customer.dto.CustomerDtos.CustomerSummaryDto;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.exception.CustomerNotFoundException;
import com.supportdesk.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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


    @Transactional(readOnly = true)
    public CustomerSummaryDto getSummary(String cif)
    {
        Customer c = getByCif(cif);
        return new CustomerSummaryDto(c.getCif(), c.getFullName(), c.getSegment(),
                maskMobile(c.getMobile()), c.getPreferredLanguage());
    }


    static String maskMobile(String mobile)
    {
        if (mobile == null || mobile.length() <= 4) {
            return "••••";
        }
        return "•".repeat(mobile.length() - 4) + mobile.substring(mobile.length() - 4);
    }
}
