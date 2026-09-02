package com.slmanju.ceylonads.auth.service;

import com.slmanju.ceylonads.auth.dto.AuthResponse;
import com.slmanju.ceylonads.auth.dto.ChangePasswordRequest;
import com.slmanju.ceylonads.auth.dto.LoginRequest;
import com.slmanju.ceylonads.auth.dto.RegisterRequest;
import com.slmanju.ceylonads.auth.entity.Account;
import com.slmanju.ceylonads.auth.entity.Role;
import com.slmanju.ceylonads.auth.repository.AccountRepository;
import com.slmanju.ceylonads.auth.security.JwtService;
import com.slmanju.ceylonads.common.exception.BadRequestException;
import com.slmanju.ceylonads.customer.entity.Customer;
import com.slmanju.ceylonads.customer.repository.CustomerRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            AccountRepository accounts,
            CustomerRepository customers,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.accounts = accounts;
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (accounts.existsByUsernameIgnoreCase(request.username())) {
            throw new BadRequestException("Username is already in use");
        }
        if (accounts.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email is already in use");
        }

        Account account = accounts.save(new Account(
                request.username().trim(),
                passwordEncoder.encode(request.password()),
                request.email().trim().toLowerCase(),
                Role.CUSTOMER));

        customers.save(new Customer(account, request.displayName().trim(), request.phone()));
        return issue(account);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        Account account = accounts.findByUsernameIgnoreCase(request.username())
                .orElseThrow();
        return issue(account);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        Account account = accounts.findByUsernameIgnoreCase(username).orElseThrow();

        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }
        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        account.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private AuthResponse issue(Account account) {
        String token = jwtService.generateToken(account);
        return new AuthResponse(token, "Bearer", jwtService.extractExpiration(token), account.getUsername(), account.getRole());
    }
}
