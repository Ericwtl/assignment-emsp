package com.emsp.assignment.domain.card.service;

import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.account.model.AccountStatus;
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
        AccountStatus accountStaus = account != null ? account.getStatus() : null;

        // 校验2: 当状态为 ACTIVATED 或 ASSIGNED 时，账户必须存在
        if (status == CardStatus.ACTIVATED || status == CardStatus.ASSIGNED) {
            if (accountEmail == null || accountEmail.isBlank() || accountStaus != AccountStatus.ACTIVATED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Account is required and must be activated for ACTIVATED or ASSIGNED cards");
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
        if (!AccountStatus.ACTIVATED.equals(account.getStatus())) {
            throw new IllegalCardOperationException("Only ACTIVATED account can be assigned");
        }

        card.setAccount(account);
        card.setStatus(CardStatus.ASSIGNED);
        cardRepository.save(card);
    }

    @Transactional
    public Card changeCardStatus(String rfidUid, CardStatus newStatus, String accountEmail) {
        Card card = cardRepository.findById(rfidUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        // 1. 新旧状态相同
        if (card.getStatus() == newStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Card is already in " + newStatus + " status");
        }

        // 2. 验证操作权限
        validatePermission(card, accountEmail);

        // 状态转换逻辑
        switch (newStatus) {
            case CREATED:
                return handleToCreated(card);
            case ASSIGNED:
                return handleToAssigned(card, accountEmail);
            case ACTIVATED:
                return handleToActivated(card, accountEmail);
            case DEACTIVATED:
                return handleToDeactivated(card);
            default:
                throw new IllegalStateException("Unknown status: " + newStatus);
        }
    }

    private void validatePermission(Card card, String accountEmail) {
        // 实际项目中应使用Spring Security进行权限验证
        if(accountEmail != null && !accountEmail.isBlank()){
            if (card.getAccount() != null && !accountEmail.equals(card.getAccount().getEmail())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You cannot assign a card to others that have already been assigned to:" + card.getAccount().getEmail());
            }
        }
    }

    private Card handleToCreated(Card card) {
        // 解除账户关联
        card.setAccount(null);
        card.setStatus(CardStatus.CREATED);
        return cardRepository.save(card);
    }

    private Card handleToAssigned(Card card, String accountEmail) {
        Account account;
        if(accountEmail == null || accountEmail.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account email is required for ASSIGNED status, please provide the account email");
        }
        if (card.getAccount() == null) {
            account = accountRepository.findById(accountEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found:" + accountEmail));
        }else{
            account = accountRepository.findById(card.getAccount().getEmail())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found:" + accountEmail));
        }

        if (account.getStatus() == AccountStatus.DEACTIVATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account must be CREATED OR ACTIVATED for ASSIGNED status: " + accountEmail + " is in DEACTIVATED STATUS");
        }

        card.setStatus(CardStatus.ASSIGNED);
        card.setAccount(account);
        return cardRepository.save(card);
    }


    private Card handleToActivated(Card card, String accountEmail) {
        Account account;
        // 必须有账户关联
        if (card.getAccount() == null) {
            if(accountEmail == null || accountEmail.isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Account email is required for ACTIVATED status, please provide the account email");
            }else{
                account = accountRepository.findById(accountEmail)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found:" + accountEmail));
            }
        }else{
            account = accountRepository.findById(card.getAccount().getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        }

        if (account.getStatus() != AccountStatus.ACTIVATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account must be ACTIVATED for ACTIVATED status");
        }

        if (card.getAccount() == null) {
            card.setAccount(account);
        }

        card.setStatus(CardStatus.ACTIVATED);
        return cardRepository.save(card);
    }

    private Card handleToDeactivated(Card card) {
        // DEACTIVATED状态转换总是允许
        card.setStatus(CardStatus.DEACTIVATED);
        return cardRepository.save(card);
    }
}