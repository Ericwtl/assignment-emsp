package com.emsp.assignment.domain.card.service;

import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import com.emsp.assignment.infrastructure.exception.AccountNotFoundException;
import com.emsp.assignment.infrastructure.exception.CardNotFoundException;
import com.emsp.assignment.infrastructure.exception.IllegalCardOperationException;
import com.emsp.assignment.infrastructure.exception.ResponseStatusException;
import com.emsp.assignment.infrastructure.persistence.AccountRepository;
import com.emsp.assignment.infrastructure.persistence.CardRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CardStateService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;

    public CardStateService(AccountRepository accountRepository,
                                 CardRepository cardRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public Card createCard(Card card) {
        // 校验1: visibleNumber 是否已存在
        if (cardRepository.existsByVisibleNumber(card.getVisibleNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Card with this visible number already exists");
        }

        // 获取状态（处理默认值）
        CardStatus status = card.getStatus() != null ? card.getStatus() : CardStatus.CREATED;
        card.setStatus(status);

        // 处理账户关联
        Account account = card.getAccount();
        String accountEmail = account != null ? account.getEmail() : null;

        // 校验2: 当状态为 ACTIVATED 或 ASSIGNED 时，账户必须存在
        if (status == CardStatus.ACTIVATED || status == CardStatus.ASSIGNED) {
            if (accountEmail == null || accountEmail.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Account is required for ACTIVATED or ASSIGNED cards");
            }

            if (!accountRepository.existsById(accountEmail)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found with email: " + accountEmail);
            }
        }

        // 校验3: 当状态为 CREATED 且提供了账户时，验证账户存在
        if (status == CardStatus.CREATED && accountEmail != null && !accountEmail.isBlank()) {
            if (!accountRepository.existsById(accountEmail)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found with email: " + accountEmail);
            }
        }

        return cardRepository.save(card);
    }

    @Transactional
    public void activateCard(String rfidUid) {
        Card card = cardRepository.findById(rfidUid.toString())
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + rfidUid));

        if (card.getAccount() == null) {
            throw new IllegalStateException("Cannot activate unassigned card");
        }

        card.activate();
        cardRepository.save(card);
    }

    @Transactional
    public void deactivateCard(String rfidUid) {
        Card card = cardRepository.findById(rfidUid).orElseThrow(
                () -> new CardNotFoundException("Card not found: " + rfidUid)
        );

        card.deactivate();
        cardRepository.save(card);
    }

    @Transactional
    public void assignCard(String rfidUid, String accountEmail) {
        Account account = accountRepository.findById(accountEmail)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountEmail));
        Card card = cardRepository.findByRfidUid(rfidUid)
                .orElseThrow(() -> new CardNotFoundException(rfidUid));

        if (!CardStatus.CREATED.equals(card.getStatus())) {
            throw new IllegalCardOperationException("Only CREATED cards can be assigned");
        }

        card.setAccount(account);
        card.setStatus(CardStatus.ASSIGNED);
        cardRepository.save(card);
    }

    @Transactional
    public void changeCardStatus(String rfidUid, CardStatus newStatus) {
        Card card = cardRepository.findByRfidUid(rfidUid)
                .orElseThrow(() -> new CardNotFoundException(rfidUid));

        // 状态转换验证逻辑
        validateStatusTransition(card.getStatus(), newStatus);

        card.setStatus(newStatus);
        cardRepository.save(card);
    }

    private void validateStatusTransition(CardStatus current, CardStatus newStatus) {
        // 实现状态机验证逻辑
        //TODO 当前卡是否已经assign给其他人
    }
}