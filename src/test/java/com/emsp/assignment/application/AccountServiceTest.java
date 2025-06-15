package com.emsp.assignment.application;

import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.account.model.AccountStatus;
import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import com.emsp.assignment.infrastructure.exception.AccountNotFoundException;
import com.emsp.assignment.infrastructure.exception.BusinessResponseException;
import com.emsp.assignment.infrastructure.exception.ConcurrentModificationException;
import com.emsp.assignment.infrastructure.exception.IllegalAccountOperationException;
import com.emsp.assignment.infrastructure.persistence.AccountRepository;
import com.emsp.assignment.infrastructure.persistence.CardRepository;
import com.emsp.assignment.domain.account.service.CardAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardAssignmentService cardAssignmentService;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private Card testCard1;
    private Card testCard2;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setEmail("test@example.com");
        testAccount.setStatus(AccountStatus.CREATED);
        testAccount.setVersion(0L);

        testCard1 = new Card();
        testCard1.setRfidUid("9572E8A1B2C3D0");
        testCard1.setAccount(testAccount);
        testCard1.setStatus(CardStatus.ASSIGNED);
        testCard1.setVisibleNumber("9999-8888-7777-6666");
        testCard1.setCreatedAt(Instant.now());

        testCard2 = new Card();
        testCard2.setRfidUid("0457E8A1C4E6G0");
        testCard2.setAccount(testAccount);
        testCard2.setStatus(CardStatus.ACTIVATED);
        testCard2.setVisibleNumber("1111-2222-3333-4444");
        testCard2.setCreatedAt(Instant.now());
    }

    @Test
    void changeAccountStatus_SameStatus_ShouldThrowBadRequestException() {
        // 测试 CREATED 和 ACTIVATED 状态
        AccountStatus[] statuses = {AccountStatus.CREATED, AccountStatus.ACTIVATED};
        for (AccountStatus status : statuses) {
            testAccount.setStatus(status);
            when(accountRepository.findByEmail(testAccount.getEmail()))
                    .thenReturn(Optional.of(testAccount));

            // 验证抛出 ResponseStatusException
            BusinessResponseException exception = assertThrows(BusinessResponseException.class, () -> {
                accountService.changeAccountStatus(testAccount.getEmail(), status, null);
            });
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Account is already in " + status + " status"));
        }

        // 单独测试 DEACTIVATED 状态
        testAccount.setStatus(AccountStatus.DEACTIVATED);
        when(accountRepository.findByEmail(testAccount.getEmail()))
                .thenReturn(Optional.of(testAccount));

        // 验证抛出 ResponseStatusException 而非 IllegalStateException
        BusinessResponseException exception = assertThrows(BusinessResponseException.class, () -> {
            accountService.changeAccountStatus(
                    testAccount.getEmail(),
                    AccountStatus.DEACTIVATED,
                    null
            );
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Account is already in DEACTIVATED status"));
    }

    @Test
    void createAccount_ShouldSaveAccount() {
        // Arrange
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        Account result = accountService.createAccount(testAccount);

        // Assert
        assertNotNull(result);
        assertEquals(testAccount.getEmail(), result.getEmail());
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void getAccountsWithCardsByLastUpdated_ShouldReturnAccounts() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        Pageable pageable = Pageable.unpaged();
        Page<Account> accountPage = new PageImpl<>(Collections.singletonList(testAccount));

        when(accountRepository.findWithCardsByLastUpdatedBetween(start, end, pageable))
                .thenReturn(accountPage);

        // Act
        Page<Account> result = accountService.getAccountsWithCardsByLastUpdated(start, end, pageable);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.getContent().size());
        verify(accountRepository, times(1))
                .findWithCardsByLastUpdatedBetween(start, end, pageable);
    }

    @Test
    void changeAccountStatus_WhenAccountNotFound_ShouldThrowException() {
        // Arrange
        String email = "nonexistent@example.com";
        when(accountRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class,
                () -> accountService.changeAccountStatus(email, AccountStatus.ACTIVATED, "AB12345678901"));
    }

    @Test
    void changeAccountStatus_FromCreatedToActivatedWithValidContractId_ShouldUpdateStatus() {
        // Arrange
        testAccount.setStatus(AccountStatus.CREATED);
        String validContractId = "DE5X7ABCD54321";

        when(accountRepository.findByEmail(testAccount.getEmail()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(cardRepository.findByAccountEmail(testAccount.getEmail()))
                .thenReturn(Collections.singletonList(testCard1));

        // Act
        accountService.changeAccountStatus(
                testAccount.getEmail(),
                AccountStatus.ACTIVATED,
                validContractId
        );

        // Assert
        assertEquals(AccountStatus.ACTIVATED, testAccount.getStatus());
        assertEquals(validContractId, testAccount.getContractId());
        verify(accountRepository, times(1)).save(testAccount);
        verify(cardRepository, times(1)).save(testCard1);
        assertEquals(CardStatus.ACTIVATED, testCard1.getStatus());
    }

    @Test
    void changeAccountStatus_FromCreatedToActivatedWithInvalidContractId_ShouldThrowException() {
        // Arrange
        testAccount.setStatus(AccountStatus.CREATED);
        String invalidContractId = "invalid";

        when(accountRepository.findByEmail(testAccount.getEmail()))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThrows(IllegalAccountOperationException.class,
                () -> accountService.changeAccountStatus(
                        testAccount.getEmail(),
                        AccountStatus.ACTIVATED,
                        invalidContractId
                ));

        assertEquals(AccountStatus.CREATED, testAccount.getStatus());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void changeAccountStatus_FromActivatedToDeactivated_ShouldUpdateStatusAndCards() {
        // Arrange
        testAccount.setStatus(AccountStatus.ACTIVATED);
        List<Card> cards = Arrays.asList(testCard1, testCard2);

        when(accountRepository.findByEmail(testAccount.getEmail()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(cardRepository.findByAccountEmail(testAccount.getEmail())).thenReturn(cards);

        // Act
        accountService.changeAccountStatus(
                testAccount.getEmail(),
                AccountStatus.DEACTIVATED,
                null
        );

        // Assert
        assertEquals(AccountStatus.DEACTIVATED, testAccount.getStatus());
        verify(accountRepository, times(1)).save(testAccount);
        verify(cardRepository, times(2)).save(any(Card.class));
        cards.forEach(card -> assertEquals(CardStatus.DEACTIVATED, card.getStatus()));
    }

    @Test
    void changeAccountStatus_FromDeactivatedToActivated_ShouldUpdateStatus() {
        // Arrange
        testAccount.setStatus(AccountStatus.DEACTIVATED);
        testAccount.setContractId("DE5X7ABCD54321");
        String validContractId = "DE5X7ABCD54321";

        when(accountRepository.findByEmail(testAccount.getEmail()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(cardRepository.findByAccountEmail(testAccount.getEmail()))
                .thenReturn(Collections.singletonList(testCard1));

        // Act
        accountService.changeAccountStatus(
                testAccount.getEmail(),
                AccountStatus.ACTIVATED,
                validContractId
        );

        // Assert
        assertEquals(AccountStatus.ACTIVATED, testAccount.getStatus());
        assertEquals(validContractId, testAccount.getContractId());
        verify(accountRepository, times(1)).save(testAccount);
        verify(cardRepository, times(1)).save(testCard1);
        assertEquals(CardStatus.ACTIVATED, testCard1.getStatus());
    }

    @Test
    void changeAccountStatus_WithOptimisticLockingFailure_ShouldThrowException() {
        // Arrange
        testAccount.setStatus(AccountStatus.CREATED);

        when(accountRepository.findByEmail(testAccount.getEmail()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Account.class, testAccount.getEmail()));

        // Act & Assert
        assertThrows(ConcurrentModificationException.class,
                () -> accountService.changeAccountStatus(
                        testAccount.getEmail(),
                        AccountStatus.ACTIVATED,
                        "DE5X7ABCD54321"
                ));
    }

    @Test
    void changeAccountStatus_InvalidStatusTransitionFromActivated_ShouldThrowException() {
        // Arrange
        testAccount.setStatus(AccountStatus.ACTIVATED);

        when(accountRepository.findByEmail(testAccount.getEmail()))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> accountService.changeAccountStatus(
                        testAccount.getEmail(),
                        AccountStatus.CREATED,
                        null
                ));

        assertEquals(AccountStatus.ACTIVATED, testAccount.getStatus());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void changeAccountStatus_InvalidStatusTransitionFromDeactivated_ShouldThrowException() {
        // Arrange
        testAccount.setStatus(AccountStatus.DEACTIVATED);

        when(accountRepository.findByEmail(testAccount.getEmail()))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> accountService.changeAccountStatus(
                        testAccount.getEmail(),
                        AccountStatus.CREATED,
                        null
                ));

        assertEquals(AccountStatus.DEACTIVATED, testAccount.getStatus());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void changeAccountStatus_SameStatus_ShouldThrowExceptionAndNotUpdate() {
        // Arrange
        AccountStatus[] statuses = {AccountStatus.CREATED, AccountStatus.ACTIVATED, AccountStatus.DEACTIVATED};

        for (AccountStatus status : statuses) {
            testAccount.setStatus(status);
            when(accountRepository.findByEmail(testAccount.getEmail()))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            BusinessResponseException exception = assertThrows(BusinessResponseException.class, () -> {
                accountService.changeAccountStatus(
                        testAccount.getEmail(),
                        status, // 相同状态
                        null
                );
            });

            // 验证异常属性
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Account is already in " + status + " status"));

            // 验证未执行任何 save 操作
            verify(accountRepository, never()).save(any(Account.class));
            verify(cardRepository, never()).save(any(Card.class));

            // 重置 Mock 调用记录，为下一次循环做准备
            reset(accountRepository, cardRepository);
        }
    }

    @Test
    void changeAccountStatus_ActivatedWithNoCards_ShouldNotUpdateCards() {
        // Arrange
        testAccount.setStatus(AccountStatus.CREATED);

        when(accountRepository.findByEmail(testAccount.getEmail()))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(cardRepository.findByAccountEmail(testAccount.getEmail()))
                .thenReturn(Collections.emptyList());

        // Act
        accountService.changeAccountStatus(
                testAccount.getEmail(),
                AccountStatus.ACTIVATED,
                "DE5X7ABCD54321"
        );

        // Assert
        assertEquals(AccountStatus.ACTIVATED, testAccount.getStatus());
        verify(accountRepository, times(1)).save(testAccount);
        verify(cardRepository, never()).save(any(Card.class));
    }
}