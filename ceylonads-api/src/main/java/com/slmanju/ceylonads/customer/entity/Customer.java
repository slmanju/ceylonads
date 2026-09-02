package com.slmanju.ceylonads.customer.entity;

import com.slmanju.ceylonads.auth.entity.Account;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Customer() {
    }

    public Customer(Account account, String displayName, String phone) {
        this.account = account;
        this.displayName = displayName;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public String getDisplayName() { return displayName; }
    public String getPhone() { return phone; }
    public Instant getCreatedAt() { return createdAt; }
}
