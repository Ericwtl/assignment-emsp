package com.emsp.assignment.interfaces.rest;


import com.emsp.assignment.application.AccountService;
import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.account.model.AccountStatus;
import com.emsp.assignment.domain.account.service.AccountStateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountStateService accountStateService;

    public AccountController(AccountService accountService,
                             AccountStateService accountStateService) {
        this.accountService = accountService;
        this.accountStateService = accountStateService;
    }

    //1.Create account.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@Valid @RequestBody Account account) {
        Account created = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(created).getBody();
    }

    @PutMapping("/{email}/status")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Account changeAccountStatus(
            @PathVariable @Valid String email,
            @RequestParam @Valid AccountStatus newStatus,
            @RequestParam(required = false) String contractId
    ) {
        Account updatedAccount = accountService.changeAccountStatus(email, newStatus, contractId);
        return updatedAccount;
    }

    @GetMapping
    public Page<Account> getAccountsByUpdateTime(
            @RequestParam("start") LocalDateTime start,
            @RequestParam("end") LocalDateTime end,
            @PageableDefault(size = 20, sort = "lastUpdated") Pageable pageable) {

        return accountService.getAccountsWithCardsByLastUpdated(start, end, pageable);
    }

    @PostMapping("/{email}/activate")
    @ResponseStatus(HttpStatus.OK)
    public void activateAccount(@PathVariable String email) {
        accountStateService.activateAccount(email);
    }

    //3.Deactivate account.
    @PostMapping("/{email}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public void deactivateAccount(@PathVariable String email) {
        accountStateService.deactivateAccount(email);
    }
}