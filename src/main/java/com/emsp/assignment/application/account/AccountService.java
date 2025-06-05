package com.emsp.assignment.application.account;


import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.infrastructure.persistence.account.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public Page<Account> getAccountsByUpdateTime(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    ) {
        return accountRepository.findByLastUpdatedBetween(startTime, endTime, pageable);
    }
}