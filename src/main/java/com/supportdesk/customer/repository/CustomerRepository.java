package com.supportdesk.customer.repository;

import com.supportdesk.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface CustomerRepository extends JpaRepository<Customer, Long>
{

    Optional<Customer> findByCifIgnoreCase(String cif);


    Optional<Customer> findByNationalId(String nationalId);

    List<Customer> findByMobile(String mobile);
}
