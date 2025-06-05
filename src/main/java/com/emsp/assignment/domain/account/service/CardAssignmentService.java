package com.emsp.assignment.domain.account.service;

import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.infrastructure.exception.AccountNotFoundException;
import com.emsp.assignment.infrastructure.persistence.account.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CardAssignmentService {

    private final AccountRepository accountRepository;

    public CardAssignmentService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void assignCardToAccount(UUID cardId, String accountEmail) {
        Account account = accountRepository.findById(accountEmail)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountEmail));

        // 实际应通过CardRepository获取卡片
        Card card = new Card();
        card.setUid(cardId);

        account.addCard(card);
        accountRepository.save(account);
    }
}