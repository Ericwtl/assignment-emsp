package com.emsp.assignment.interfaces.rest;

import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
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

    // 1.Create card.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Card createCard(@RequestBody CardRequest request) {
        return cardStateService.createCard(request.getRfidUid(), request.getVisibleNumber());
    }

    // Active card.
    @PostMapping("/{rfidUid}/activate")
    @ResponseStatus(HttpStatus.OK)
    public void activateCard(@PathVariable String rfidUid) {
        cardStateService.activateCard(rfidUid);
    }

    // Deactivate card.
    @PostMapping("/{rfidUid}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public void deactivateCard(@PathVariable String rfidUid) {
        cardStateService.deactivateCard(rfidUid);
    }

    @PostMapping("/{rfidUid}/assign")
    public void assignCardToAccount(
            @PathVariable String rfidUid,
            @RequestParam String accountEmail
    ) {
        cardStateService.assignCard(rfidUid, accountEmail);
    }

    @PostMapping("/{rfidUid}/status")
    public void changeCardStatus(
            @PathVariable String rfidUid,
            @RequestParam CardStatus newStatus
    ) {
        cardStateService.changeCardStatus(rfidUid, newStatus);
    }

    public static class CardRequest {
        private String rfidUid;
        private String visibleNumber;

        public String getRfidUid() {
            return rfidUid;
        }

        public void setRfidUid(String rfidUid) {
            this.rfidUid = rfidUid;
        }

        public String getVisibleNumber() {
            return visibleNumber;
        }

        public void setVisibleNumber(String visibleNumber) {
            this.visibleNumber = visibleNumber;
        }
    }

}