package com.slmanju.ceylonads.customer.mapper;

import com.slmanju.ceylonads.customer.dto.CustomerResponse;
import com.slmanju.ceylonads.customer.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getAccount().getUsername(),
                customer.getAccount().getEmail(),
                customer.getDisplayName(),
                customer.getPhone(),
                customer.getAccount().getStatus());
    }
}
