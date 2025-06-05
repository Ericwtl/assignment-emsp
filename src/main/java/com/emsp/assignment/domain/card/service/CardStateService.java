package com.emsp.assignment.domain.card.service;

import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.infrastructure.exception.CardNotFoundException;
import com.emsp.assignment.infrastructure.persistence.card.CardRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CardStateService {

    private final CardRepository cardRepository;

    public CardStateService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Transactional
    public Card createCard(byte[] encryptedCard, String lastFourDigits) {
        Card card = new Card();
        card.setEncryptedCard(encryptedCard);
        card.setLastFourDigits(lastFourDigits);
        return cardRepository.save(card);
    }

    @Transactional
    public void activateCard(UUID cardId) {
        Card card = cardRepository.findById(cardId.toString())
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardId));

        if (card.getAccount() == null) {
            throw new IllegalStateException("Cannot activate unassigned card");
        }

        card.activate();
        cardRepository.save(card);
    }

    @Transactional
    public void deactivateCard(String cardId) {
        Card card = cardRepository.findById(cardId).orElseThrow(
                () -> new CardNotFoundException("Card not found: " + cardId)
        );

        card.deactivate();
        cardRepository.save(card);
    }
}