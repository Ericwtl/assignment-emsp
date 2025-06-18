package com.emsp.assignment.interfaces.rest;

import com.emsp.assignment.domain.account.model.AccountStatus;
import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import com.emsp.assignment.domain.card.service.CardStateService;
import jakarta.validation.Valid;
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
    public Card createCard(@RequestBody @Valid Card card) {
        return cardStateService.createCard(card);
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

    @PutMapping("/{rfidUid}/assign")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Card assignCardToAccount(
            @PathVariable String rfidUid,
            @RequestParam @Valid String accountEmail
    ) {
        Card card = cardStateService.assignCard(rfidUid, accountEmail);
        return card;
    }

    @PutMapping("/{rfidUid}/status")    // POST /api/accounts/test@example.com/status
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Card changeCardStatus(
            @PathVariable String rfidUid,
            @RequestParam CardStatus newStatus,
            @RequestParam @Valid String accountEmail
    ) {
        Card card = cardStateService.changeCardStatus(rfidUid, newStatus, accountEmail);
        return card;
    }
}