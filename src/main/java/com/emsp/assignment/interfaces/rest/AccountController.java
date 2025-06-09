package com.emsp.assignment.interfaces.rest;


import com.emsp.assignment.application.AccountService;
import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.account.model.AccountStatus;
import com.emsp.assignment.domain.account.service.AccountStateService;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;

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
    public Account createAccount(@RequestBody Account account) {
        //log.info("RECEIVED ACCOUNT: {}", account.getEmail()); // 添加此日志
        return accountService.createAccount(account);
    }

    @PutMapping("/{email}/status")    // POST /api/accounts/test@example.com/status
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void changeAccountStatus(
            @PathVariable String email,
            @RequestParam AccountStatus newStatus,
            @RequestParam(required = false) String contractId
    ) {
        accountService.changeAccountStatus(email, newStatus, contractId);
    }

    @GetMapping
    public Page<Account> getAccountsByUpdateTime(
            @RequestParam("start") LocalDateTime start,
            @RequestParam("end") LocalDateTime end,
            @PageableDefault(size = 20, sort = "lastUpdated") Pageable pageable) {

        return accountService.getAccountsWithCardsByLastUpdated(start, end, pageable);
    }
}