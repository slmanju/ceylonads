package com.slmanju.ceylonads.auth.security;

import com.slmanju.ceylonads.auth.entity.Account;
import com.slmanju.ceylonads.auth.entity.AccountStatus;
import com.slmanju.ceylonads.auth.repository.AccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accounts;

    public CustomUserDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown username: " + username));
        return toUserDetails(account);
    }

    private UserDetails toUserDetails(Account account) {
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .authorities("ROLE_" + account.getRole().name())
                .disabled(account.getStatus() == AccountStatus.DISABLED)
                .accountLocked(account.getStatus() == AccountStatus.SUSPENDED)
                .build();
    }
}
