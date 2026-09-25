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

    /** SAGA Step 1 deduct balance**/

    @PutMapping("{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(@PathVariable String accountNumber,@RequestParam BigDecimal amount){
        accountService.deductBalance(accountNumber,amount);
        return ResponseEntity.ok("Balance deducted successfully");
    }

    /* SAGA ste 4 compensating transaction endpoint
    * CALLED BY tranaction service in two scenarios:
    * 1.fraud detected -> refund sender
    * 2.transaction completed -> credit receiver
    * */

    @PutMapping("{accountNumber}/credit")
    public ResponseEntity<String> creditBalance(@PathVariable String accountNumber,@RequestParam BigDecimal amount){
        accountService.creditBalance(accountNumber,amount);
        return ResponseEntity.ok("Balance credited successfully");
    }


}
