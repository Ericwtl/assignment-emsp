package com.emsp.assignment.interfaces.rest.card;

import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.service.CardStateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardStateService cardStateService;

    public CardController(CardStateService cardStateService) {
        this.cardStateService = cardStateService;
    }

    // 创建卡
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Card createCard(@RequestBody CardRequest request) {
        return cardStateService.createCard(request.getEncryptedCard(), request.getLastFourDigits());
    }

    // 更改卡状态
    @PostMapping("/{cardId}/activate")
    @ResponseStatus(HttpStatus.OK)
    public void activateCard(@PathVariable UUID cardId) {
        cardStateService.activateCard(cardId);
    }

    public static class CardRequest {
        private byte[] encryptedCard;
        private String lastFourDigits;

        public byte[] getEncryptedCard() {
            return encryptedCard;
        }

        public void setEncryptedCard(byte[] encryptedCard) {
            this.encryptedCard = encryptedCard;
        }

        public void setLastFourDigits(String lastFourDigits) {
            this.lastFourDigits = lastFourDigits;
        }

        public String getLastFourDigits() {
            return lastFourDigits;
        }
    }
}