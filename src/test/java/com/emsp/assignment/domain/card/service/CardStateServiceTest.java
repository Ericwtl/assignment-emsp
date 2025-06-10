package com.emsp.assignment.domain.card.service;

import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.account.model.AccountStatus;
import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import com.emsp.assignment.infrastructure.exception.*;
import com.emsp.assignment.infrastructure.persistence.AccountRepository;
import com.emsp.assignment.infrastructure.persistence.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardStateServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardStateService cardStateService;

    private Card testCard;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testCard = new Card();
        testCard.setRfidUid(UUID.randomUUID().toString());
        testCard.setVisibleNumber("1234-5678-9012-3456");
        testCard.setStatus(CardStatus.CREATED);
        testCard.setCreatedAt(Instant.now());

        testAccount = new Account();
        testAccount.setEmail("test@example.com");
        testAccount.setStatus(AccountStatus.ACTIVATED);
    }

    @Test
    void createCard_WithExistingVisibleNumber_ShouldThrowBadRequest() {
        when(cardRepository.existsByVisibleNumber(testCard.getVisibleNumber())).thenReturn(true);

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.createCard(testCard));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("visible number already exists"));
    }

    @Test
    void createCard_WithActivatedStatusAndNullAccount_ShouldThrowBadRequest() {
        testCard.setStatus(CardStatus.ACTIVATED);
        testCard.setAccount(null);

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.createCard(testCard));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Account is required"));
    }

    @Test
    void createCard_WithActivatedStatusAndNonActivatedAccount_ShouldThrowBadRequest() {
        testCard.setStatus(CardStatus.ACTIVATED);
        testAccount.setStatus(AccountStatus.CREATED);
        testCard.setAccount(testAccount);

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.createCard(testCard));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("must be activated"));
    }

    @Test
    void createCard_WithActivatedStatusAndNonExistentAccount_ShouldThrowNotFound() {
        testCard.setStatus(CardStatus.ACTIVATED);
        testCard.setAccount(testAccount);

        when(accountRepository.existsById(testAccount.getEmail())).thenReturn(false);

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.createCard(testCard));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Account not found"));
    }

    @Test
    void createCard_WithCreatedStatusAndNonExistentAccount_ShouldThrowNotFound() {
        testCard.setStatus(CardStatus.CREATED);
        testCard.setAccount(testAccount);

        when(accountRepository.existsById(testAccount.getEmail())).thenReturn(false);

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.createCard(testCard));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Account not found"));
    }

    @Test
    void createCard_WithValidData_ShouldSaveCard() {
        testCard.setStatus(CardStatus.CREATED);
        testCard.setAccount(null);

        when(cardRepository.existsByVisibleNumber(testCard.getVisibleNumber())).thenReturn(false);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardStateService.createCard(testCard);

        assertNotNull(result);
        assertEquals(CardStatus.CREATED, result.getStatus());
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void activateCard_NonExistentCard_ShouldThrowException() {
        String rfidUid = UUID.randomUUID().toString();
        when(cardRepository.findById(rfidUid)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardStateService.activateCard(rfidUid));
    }

    @Test
    void activateCard_UnassignedCard_ShouldThrowException() {
        testCard.setAccount(null);
        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));

        assertThrows(IllegalStateException.class,
                () -> cardStateService.activateCard(testCard.getRfidUid()));
    }

    @Test
    void activateCard_ValidCard_ShouldUpdateStatus() {
        testCard.setAccount(testAccount);
        testCard.setStatus(CardStatus.ASSIGNED);

        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        cardStateService.activateCard(testCard.getRfidUid());

        assertEquals(CardStatus.ACTIVATED, testCard.getStatus());
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void deactivateCard_NonExistentCard_ShouldThrowException() {
        String rfidUid = UUID.randomUUID().toString();
        when(cardRepository.findById(rfidUid)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardStateService.deactivateCard(rfidUid));
    }

    @Test
    void deactivateCard_ValidCard_ShouldUpdateStatus() {
        testCard.setStatus(CardStatus.ACTIVATED);
        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        cardStateService.deactivateCard(testCard.getRfidUid());

        assertEquals(CardStatus.DEACTIVATED, testCard.getStatus());
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void assignCard_NonExistentAccount_ShouldThrowException() {
        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> cardStateService.assignCard(testCard.getRfidUid(), testAccount.getEmail()));
    }

    @Test
    void assignCard_NonExistentCard_ShouldThrowException() {
        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.of(testAccount));
        when(cardRepository.findByRfidUid(testCard.getRfidUid())).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardStateService.assignCard(testCard.getRfidUid(), testAccount.getEmail()));
    }

    @Test
    void assignCard_NonCreatedCard_ShouldThrowException() {
        testCard.setStatus(CardStatus.ASSIGNED);
        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.of(testAccount));
        when(cardRepository.findByRfidUid(testCard.getRfidUid())).thenReturn(Optional.of(testCard));

        assertThrows(IllegalCardOperationException.class,
                () -> cardStateService.assignCard(testCard.getRfidUid(), testAccount.getEmail()));
    }

    @Test
    void assignCard_NonActivatedAccount_ShouldThrowException() {
        testAccount.setStatus(AccountStatus.DEACTIVATED);
        testCard.setStatus(CardStatus.CREATED);

        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.of(testAccount));
        when(cardRepository.findByRfidUid(testCard.getRfidUid())).thenReturn(Optional.of(testCard));

        assertThrows(IllegalCardOperationException.class,
                () -> cardStateService.assignCard(testCard.getRfidUid(), testAccount.getEmail()));
    }

    @Test
    void assignCard_ValidData_ShouldUpdateCard() {
        testCard.setStatus(CardStatus.CREATED);
        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.of(testAccount));
        when(cardRepository.findByRfidUid(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        cardStateService.assignCard(testCard.getRfidUid(), testAccount.getEmail());

        assertEquals(CardStatus.ASSIGNED, testCard.getStatus());
        assertEquals(testAccount.getEmail(), testCard.getAccount().getEmail());
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void changeCardStatus_NonExistentCard_ShouldThrowNotFound() {
        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.empty());

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.changeCardStatus(
                        testCard.getRfidUid(), CardStatus.ACTIVATED, testAccount.getEmail()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Card not found"));
    }

    @Test
    void changeCardStatus_SameStatus_ShouldThrowBadRequest() {
        testCard.setStatus(CardStatus.ACTIVATED);
        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.changeCardStatus(
                        testCard.getRfidUid(), CardStatus.ACTIVATED, testAccount.getEmail()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("already in ACTIVATED status"));
    }

    @Test
    void changeCardStatus_ToCreated_ShouldUpdateStatus() {
        testCard.setStatus(CardStatus.ASSIGNED);
        testCard.setAccount(testAccount);

        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardStateService.changeCardStatus(
                testCard.getRfidUid(), CardStatus.CREATED, testAccount.getEmail());

        assertEquals(CardStatus.CREATED, result.getStatus());
        assertNull(result.getAccount());
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void changeCardStatus_ToAssigned_WithoutAccountEmail_ShouldThrowBadRequest() {
        testCard.setStatus(CardStatus.CREATED);
        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.changeCardStatus(
                        testCard.getRfidUid(), CardStatus.ASSIGNED, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Account email is required"));
    }

    @Test
    void changeCardStatus_ToAssigned_WithNonExistentAccount_ShouldThrowNotFound() {
        testCard.setStatus(CardStatus.CREATED);
        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.empty());

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.changeCardStatus(
                        testCard.getRfidUid(), CardStatus.ASSIGNED, testAccount.getEmail()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Account not found"));
    }

    @Test
    void changeCardStatus_ToAssigned_WithDeactivatedAccount_ShouldThrowBadRequest() {
        testCard.setStatus(CardStatus.CREATED);
        testAccount.setStatus(AccountStatus.DEACTIVATED);

        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.of(testAccount));

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.changeCardStatus(
                        testCard.getRfidUid(), CardStatus.ASSIGNED, testAccount.getEmail()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("must be CREATED OR ACTIVATED"));
    }

    @Test
    void changeCardStatus_ToAssigned_WithValidData_ShouldUpdateCard() {
        testCard.setStatus(CardStatus.CREATED);
        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.of(testAccount));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardStateService.changeCardStatus(
                testCard.getRfidUid(), CardStatus.ASSIGNED, testAccount.getEmail());

        assertEquals(CardStatus.ASSIGNED, result.getStatus());
        assertEquals(testAccount.getEmail(), result.getAccount().getEmail());
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void changeCardStatus_ToActivated_WithoutAccount_AndNullEmail_ShouldThrowBadRequest() {
        testCard.setStatus(CardStatus.CREATED);
        testCard.setAccount(null);

        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.changeCardStatus(
                        testCard.getRfidUid(), CardStatus.ACTIVATED, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Account email is required"));
    }

    @Test
    void changeCardStatus_ToActivated_WithoutAccount_AndNonExistentAccount_ShouldThrowNotFound() {
        testCard.setStatus(CardStatus.CREATED);
        testCard.setAccount(null);

        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.empty());

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.changeCardStatus(
                        testCard.getRfidUid(), CardStatus.ACTIVATED, testAccount.getEmail()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Account not found"));
    }

    @Test
    void changeCardStatus_ToActivated_WithDeactivatedAccount_ShouldThrowBadRequest() {
        testCard.setStatus(CardStatus.CREATED);
        testCard.setAccount(testAccount);
        testAccount.setStatus(AccountStatus.DEACTIVATED);

        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.of(testAccount));

        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.changeCardStatus(
                        testCard.getRfidUid(), CardStatus.ACTIVATED, testAccount.getEmail()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("must be ACTIVATED"));
    }

    @Test
    void changeCardStatus_ToActivated_WithValidData_ShouldUpdateCard() {
        testCard.setStatus(CardStatus.ASSIGNED);
        testCard.setAccount(testAccount);

        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(accountRepository.findById(testAccount.getEmail())).thenReturn(Optional.of(testAccount));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardStateService.changeCardStatus(
                testCard.getRfidUid(), CardStatus.ACTIVATED, testAccount.getEmail());

        assertEquals(CardStatus.ACTIVATED, result.getStatus());
        assertEquals(testAccount.getEmail(), result.getAccount().getEmail());
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void changeCardStatus_ToDeactivated_ShouldUpdateStatus() {
        testCard.setStatus(CardStatus.ACTIVATED);

        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardStateService.changeCardStatus(
                testCard.getRfidUid(), CardStatus.DEACTIVATED, testAccount.getEmail());

        assertEquals(CardStatus.DEACTIVATED, result.getStatus());
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void validatePermission_WithDifferentAccount_ShouldThrowForbidden() {
        testCard.setAccount(testAccount);
        String differentEmail = "another@example.com";
        when(cardRepository.findById(testCard.getRfidUid())).thenReturn(Optional.of(testCard));
        BusinessResponseException exception = assertThrows(BusinessResponseException.class,
                () -> cardStateService.changeCardStatus(
                        testCard.getRfidUid(), CardStatus.ACTIVATED, differentEmail));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getMessage().contains("You cannot assign a card to others"));
    }
}