package com.slmanju.ceylonads.customer.repository;

import com.slmanju.ceylonads.customer.entity.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByAccountUsernameIgnoreCase(String username);

    // CustomerMapper touches account for every customer (username/email/status); fetch it here
    // instead of lazily per customer when mapping the admin "all customers" list.
    @EntityGraph(attributePaths = "account")
    @Override
    List<Customer> findAll();
}
