package com.bank.transactionservice.Service;

import com.bank.transactionservice.Client.AccountServiceClient;
import com.bank.transactionservice.DTO.TransactionResponse;
import com.bank.transactionservice.DTO.TransferRequest;
import com.bank.transactionservice.Entity.Transaction;
import com.bank.transactionservice.Entity.TransactionStatus;
import com.bank.transactionservice.Entity.TransactionType;
import com.bank.transactionservice.Repository.TransactionRepository;
import com.bank.transactionservice.TransactionServiceApplication;
import jakarta.validation.Valid;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;

    private static final String TRANSACTION_INITIATED_TOPIC="transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC="transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC="transaction.refunded";
    public TransactionResponse verifyOTP(String transactionId, String otp) {
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
    }

    public TransactionResponse getTransaction(String transactionId) {
    }

    /* SAGA step-1
      initiate transfer
      deducts from sender via feign
      saves transaction as processing
      publish event to kafka for fraud check
      returns
     */

    public TransactionResponse transfer(@Valid TransferRequest request) {
        log.info("SAGA START-Transfer:{}->{} amount:{}",
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount()
                );

        //SAGA step1: deduct from sender

        accountServiceClient.deductBalance(
                request.getSenderAccountNumber(),
                request.getAmount()
        );

        Transaction transaction=new Transaction();

        transaction.setSenderAccountNumber(request.getSenderAccountNumber());
        transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setDescription(request.getDescription());
        transaction.setReferenceNumber(UUID.randomUUID().toString());

        Transaction savedTransaction=transactionRepository.save(transaction);
        log.info("Transaction saved as PROCESSING:{}",savedTransaction.getId());

    }
}
