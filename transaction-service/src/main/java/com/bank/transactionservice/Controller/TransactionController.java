package com.bank.transactionservice.Controller;


import com.bank.transactionservice.DTO.TransactionResponse;
import com.bank.transactionservice.DTO.TransferRequest;
import com.bank.transactionservice.Service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@Slf4j
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request));

    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId){
        return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<TransactionResponse> getTransactionHistory(@PathVariable String accountNumber){
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber));
    }
}
