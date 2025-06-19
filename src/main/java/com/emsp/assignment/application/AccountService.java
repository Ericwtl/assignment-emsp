package com.emsp.assignment.application;


import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.account.model.AccountStatus;
import com.emsp.assignment.domain.account.service.CardAssignmentService;
import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import com.emsp.assignment.infrastructure.exception.*;
import com.emsp.assignment.infrastructure.persistence.AccountRepository;
import com.emsp.assignment.infrastructure.persistence.CardRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        Optional<Account> existingAccount = accountRepository.findByEmail(account.getEmail());
        if (existingAccount.isPresent()) {
            throw new EmailAlreadyExistsException("Email: " + account.getEmail() + " already exists");
        }
        return accountRepository.save(account);
    }

    public Page<Account> getAccountsWithCardsByLastUpdated(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable) {
        return accountRepository.findWithCardsByLastUpdatedBetween(start, end, pageable);
    }

    @Transactional
    public Account  changeAccountStatus(String email, AccountStatus newStatus, String contractId) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + email));

        validateStatusTransition(account.getStatus(), newStatus, contractId);


        try {
            if (account.getStatus() == AccountStatus.CREATED && newStatus == AccountStatus.ACTIVATED) {
                account.setContractId(contractId);
            }
            account.setStatus(newStatus);
            accountRepository.save(account);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentModificationException("The account status has been updated by another operation, please try again.");
        }

        List<Card> cards = cardRepository.findByAccountEmail(email);
        if (!cards.isEmpty()) {
            if (newStatus == AccountStatus.ACTIVATED) {
                cards.stream()
                        .filter(card -> card.getStatus() == CardStatus.ASSIGNED)
                        .forEach(card -> {
                            card.setStatus(CardStatus.ACTIVATED);
                            cardRepository.save(card);
                        });
            }
            else if (newStatus == AccountStatus.DEACTIVATED) {
                cards.forEach(card -> {
                    card.setStatus(CardStatus.DEACTIVATED);
                    cardRepository.save(card);
                });
            }
        }
        return account;
    }

    private void validateStatusTransition(AccountStatus current, AccountStatus newStatus, String contractId) {

        if (current == newStatus) {
            throw new BusinessResponseException(HttpStatus.BAD_REQUEST,
                    "Account is already in " + newStatus + " status");
        }

        switch (current) {
            case CREATED:
                if (newStatus == AccountStatus.ACTIVATED) {
                    if (contractId == null || !contractId.matches("^[A-Z]{2}[0-9A-Z]{3}[0-9A-Z]{9}$")) {
                        throw new IllegalAccountOperationException("Valid contract ID required to activate account");
                    }
                }

                break;
            case ACTIVATED:
                if (newStatus != AccountStatus.DEACTIVATED) {
                    throw new IllegalStateException("Active accounts can only be deactivated");
                }
                break;

            case DEACTIVATED:
                if (newStatus != AccountStatus.ACTIVATED) {
                    throw new IllegalStateException("Deactivated accounts can only be reactivated");
                }
                break;
        }
    }
}