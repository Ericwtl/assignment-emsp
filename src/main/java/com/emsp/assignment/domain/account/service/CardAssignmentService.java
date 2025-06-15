package com.emsp.assignment.domain.account.service;

import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import com.emsp.assignment.infrastructure.exception.AccountNotFoundException;
import com.emsp.assignment.infrastructure.exception.CardNotFoundException;
import com.emsp.assignment.infrastructure.persistence.AccountRepository;
import com.emsp.assignment.infrastructure.persistence.CardRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

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