package com.slmanju.ceylonads.customer.repository;

import com.slmanju.ceylonads.customer.entity.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByAccountUsernameIgnoreCase(String username);

    // Row lock used to serialize a per-customer counted-limit check-then-create (e.g. Tuition's
    // max-15-concurrent-listings rule - see TuitionClassService.create) for the rest of the
    // caller's transaction, so two simultaneous requests from the same customer can't both pass
    // the same count check and together exceed the limit.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> lockById(@Param("id") Long id);

    // CustomerMapper touches account for every customer (username/email/status); fetch it here
    // instead of lazily per customer when mapping the admin "all customers" list.
    @EntityGraph(attributePaths = "account")
    @Override
    List<Customer> findAll();
}
