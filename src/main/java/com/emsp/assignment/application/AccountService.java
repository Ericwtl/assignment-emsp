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
        // 校验邮箱是否已存在
        Optional<Account> existingAccount = accountRepository.findByEmail(account.getEmail());
        if (existingAccount.isPresent()) {
            // 这里先抛出自定义异常，后面讲异常处理
            throw new EmailAlreadyExistsException("Email: " + account.getEmail() + " already exists");
        }
        return accountRepository.save(account);
    }

    public Page<Account> getAccountsWithCardsByLastUpdated(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable) {
        // 使用JOIN FETCH避免N+1问题
        return accountRepository.findWithCardsByLastUpdatedBetween(start, end, pageable);
    }

    @Transactional
    public Account  changeAccountStatus(String email, AccountStatus newStatus, String contractId) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + email));

        // 状态转换验证逻辑
        validateStatusTransition(account.getStatus(), newStatus, contractId);


        // 3. 执行账户状态更新
        try {
            // 如果是从CREATED到ACTIVATED的转换
            if (account.getStatus() == AccountStatus.CREATED && newStatus == AccountStatus.ACTIVATED) {
                // 更新合同ID
                account.setContractId(contractId);
            }
            account.setStatus(newStatus);
            accountRepository.save(account); // 自动进行版本检查
        } catch (OptimisticLockingFailureException ex) {
            // 处理并发冲突
            throw new ConcurrentModificationException("The account status has been updated by another operation, please try again.");
        }

        // 4. 账户状态与卡片状态联动处理
        List<Card> cards = cardRepository.findByAccountEmail(email);
        if (!cards.isEmpty()) {
            // 4.1 激活账户时自动激活ASSIGNED卡
            if (newStatus == AccountStatus.ACTIVATED) {
                cards.stream()
                        .filter(card -> card.getStatus() == CardStatus.ASSIGNED)
                        .forEach(card -> {
                            card.setStatus(CardStatus.ACTIVATED);
                            cardRepository.save(card);
                        });
            }
            // 4.2 停用账户时强制停用所有卡
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
                // 激活账户必须提供有效的合同ID
                if (newStatus == AccountStatus.ACTIVATED) {
                    if (contractId == null || !contractId.matches("^[A-Z]{2}[0-9A-Z]{3}[0-9A-Z]{9}$")) {
                        throw new IllegalAccountOperationException("Valid contract ID required to activate account");
                    }
                }

                break;
            case ACTIVATED:
                // ACTIVATED只能转为DEACTIVATED
                if (newStatus != AccountStatus.DEACTIVATED) {
                    throw new IllegalStateException("Active accounts can only be deactivated");
                }
                break;

            case DEACTIVATED:
                // DEACTIVATED可以重新激活
                if (newStatus != AccountStatus.ACTIVATED) {
                    throw new IllegalStateException("Deactivated accounts can only be reactivated");
                }
                break;
        }
    }
}