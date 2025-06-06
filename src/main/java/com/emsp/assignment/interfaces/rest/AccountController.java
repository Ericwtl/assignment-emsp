package com.emsp.assignment.interfaces.rest;


import com.emsp.assignment.application.AccountService;
import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.account.service.AccountStateService;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

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

    //2.Active account.
    @PostMapping("/{email}/activate") // POST /api/accounts/test@example.com/activate
    @ResponseStatus(HttpStatus.OK)
    public void activateAccount(@PathVariable String email) {
        accountStateService.activateAccount(email);
    }

    //3.Deactivate account.
    @PostMapping("/{email}/deactivate") // POST /api/accounts/test@example.com/deactivate
    @ResponseStatus(HttpStatus.OK)
    public void deactivateAccount(@PathVariable String email) {
        accountStateService.deactivateAccount(email);
    }

    @GetMapping  // GET /api/accounts?start=2023-01-01T00:00:00&end=2023-12-31T23:59:59&page=0&size=20
    public Page<Account> getAccountsByUpdateTime(
            @RequestParam("start") LocalDateTime start,
            @RequestParam("end") LocalDateTime end,
            @PageableDefault(size = 20, sort = "lastUpdated") Pageable pageable) {

        return accountService.getAccountsWithCardsByLastUpdated(start, end, pageable);
    }
}