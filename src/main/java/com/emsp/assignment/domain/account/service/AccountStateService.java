package com.emsp.assignment.domain.account.service;


import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.infrastructure.exception.AccountNotFoundException;
import com.emsp.assignment.infrastructure.persistence.account.AccountRepository;
import com.emsp.assignment.infrastructure.persistence.card.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountStateService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;

    public AccountStateService(AccountRepository accountRepository,CardRepository cardRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public void activateAccount(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with email: " + email));

        account.activate();
        accountRepository.save(account);
    }

    @Transactional
    public void deactivateAccount(String email) {
        Account account = accountRepository.findByEmail(email).
                orElseThrow(() -> new AccountNotFoundException("Account not found with email: " + email));

        account.deactivate();
        // 级联停用所有关联卡
        account.getCards().forEach(card -> {
            card.deactivate();
            cardRepository.save(card);
        });
        accountRepository.save(account);
    }
}