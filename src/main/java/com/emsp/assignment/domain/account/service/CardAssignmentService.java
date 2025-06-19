package com.emsp.assignment.domain.account.service;

import com.emsp.assignment.infrastructure.persistence.AccountRepository;
import com.emsp.assignment.infrastructure.persistence.CardRepository;
import org.springframework.stereotype.Service;

@Service
public class CardAssignmentService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;

    public CardAssignmentService(AccountRepository accountRepository,
                                 CardRepository cardRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
    }

}