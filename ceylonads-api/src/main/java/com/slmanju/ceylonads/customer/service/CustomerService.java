package com.slmanju.ceylonads.customer.service;

import com.slmanju.ceylonads.auth.entity.AccountStatus;
import com.slmanju.ceylonads.common.exception.NotFoundException;
import com.slmanju.ceylonads.customer.dto.CustomerResponse;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.mapper.CustomerMapper;
import com.slmanju.ceylonads.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customers, CustomerMapper customerMapper) {
        this.customers = customers;
        this.customerMapper = customerMapper;
    }

    @Transactional(readOnly = true)
    public Customer requireByUsername(String username) {
        return customers.findByAccountUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    @Transactional(readOnly = true)
    public Customer requireById(Long id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    @Transactional(readOnly = true)
    public CustomerResponse me(String username) {
        return customerMapper.toResponse(requireByUsername(username));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customers.findAll().stream().map(customerMapper::toResponse).toList();
    }

    @Transactional
    public CustomerResponse updateStatus(Long id, AccountStatus status) {
        Customer customer = customers.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        customer.getAccount().setStatus(status);
        return customerMapper.toResponse(customer);
    }
}
