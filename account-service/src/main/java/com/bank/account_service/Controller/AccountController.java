package com.bank.account_service.Controller;


import com.bank.account_service.DTO.AccountResponse;
import com.bank.account_service.DTO.CreateAccountRequest;
import com.bank.account_service.Service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {


    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request){

        return  ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.getAccount(accountNumber));

    }


    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.getAccount(accountNumber));

    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> bockAccount(@PathVariable String accountNumber){
        accountService.blockAccount;
        return ResponseEntity.ok("Account Blocked successfully");

    }

    /** SAGA Steps **/
}
