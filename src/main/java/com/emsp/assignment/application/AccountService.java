package com.emsp.assignment.application;


import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.account.service.CardAssignmentService;
import com.emsp.assignment.infrastructure.persistence.AccountRepository;
import com.emsp.assignment.infrastructure.persistence.CardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CardAssignmentService cardAssignmentService;

    public AccountService(AccountRepository accountRepository,
                                     CardRepository cardRepository,
                                     CardAssignmentService cardAssignmentService) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.cardAssignmentService = cardAssignmentService;
    }

    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public Page<Account> getAccountsByUpdateTime(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable) {
        return accountRepository.findByLastUpdatedBetween(startTime, endTime, pageable);
    }

    public Page<Account> getAccountsWithCardsByLastUpdated(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable) {
        // 使用JOIN FETCH避免N+1问题
        return accountRepository.findWithCardsByLastUpdatedBetween(start, end, pageable);
    }
}