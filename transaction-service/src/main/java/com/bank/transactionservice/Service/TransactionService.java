package com.bank.transactionservice.Service;

import com.bank.transactionservice.Client.AccountServiceClient;
import com.bank.transactionservice.DTO.TransactionResponse;
import com.bank.transactionservice.DTO.TransferRequest;
import com.bank.transactionservice.Entity.Transaction;
import com.bank.transactionservice.Entity.TransactionStatus;
import com.bank.transactionservice.Entity.TransactionType;
import com.bank.transactionservice.Repository.TransactionRepository;
import com.bank.transactionservice.TransactionServiceApplication;
import com.bank.transactionservice.event.TransactionCompletedEvent;
import com.bank.transactionservice.event.TransactionInitiatedEvent;
import jakarta.validation.Valid;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC="transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC="transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC="transaction.refunded";
    private final RedisTemplate<String,String>redisTemplate;
    private static final String FRAUD_DETECTED_TOPIC="fraud.detected";
//    public TransactionResponse verifyOTP(String transactionId, String otp) {
//    }
//
    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        return transactionRepository
                .findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransaction(String transactionId) {

        return mapToResponse(transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction Not Found !"+transactionId
        )));


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

        TransactionInitiatedEvent event=new TransactionInitiatedEvent(
                savedTransaction.getId(),
                savedTransaction.getSenderAccountNumber(),
                savedTransaction.getReceiverAccountNumber(),
                savedTransaction.getAmount(),
                savedTransaction.getDescription()

        );

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC,savedTransaction.getId(),event);
        log.info("SAGA step 2 - Transaction InititatedEvent published:{}",savedTransaction.getId());

        return mapToResponse(savedTransaction);



    }

    private TransactionResponse mapToResponse(Transaction transaction){
        TransactionResponse response=new TransactionResponse();
        response.setId(transaction.getId());
        response.setSenderAccountNumber(transaction.getSenderAccountNumber());
        response.setReceiverAccountNumber(transaction.getReceiverAccountNumber());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());

        return response;

    }

    public TransactionResponse verifyOTP(String transactionId, String otp) {
        log.info("otp verification for the transaction:{}",transactionId);


        Transaction transaction=transactionRepository.findById(transactionId)
        .orElseThrow(()->new RuntimeException(
                "Transaction not found"+transactionId
        ));

        String otpKey="verifiaction:otp"+transactionId;
        String storedOtp=redisTemplate.opsForValue().get(otpKey);

        if(storedOtp==null){
            log.warn("OTP expired for transaction:{}",transactionId);
            compensateTransaction(transaction,"OTP expired -transaction cancelled and amount refunded");
            return mapToResponse(transaction);
        }

        if(!storedOtp.equals(otp)){
            log.warn("Wrong otp=blocking account and refunding:{}",transactionId);
            redisTemplate.delete(otpKey);
            blockAccountAndCompensate(transaction,
                    "wrong otp entered- transaction cancelled,"+
                    "account blocked for security");
            return mapToResponse(transaction);
        }
        log.info("OTP verified-completing transaction :{}",transactionId);
        redisTemplate.delete(otpKey);
        completeTransaction(transaction);
        return mapToResponse(transaction);
    }

    private void completeTransaction(Transaction transaction) {
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        TransactionCompletedEvent completedEvent=new TransactionCompletedEvent(
                transaction.getId(),
                transaction.getSenderAccountNumber(),
                transaction.getReceiverAccountNumber(),
                transaction.getAmount(),
                transaction.getDescription()

        );
        kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC,transaction.getId(),completedEvent);

        log.info("SAGA COMPLETE-Transaction{} completed",
                transaction.getId());


    }

    private void blockAccountAndCompensate(Transaction transaction, String reason) {

        //publish fraud.detected ->account service will block the account
        Map<String,Object> fraudEvent=new HashMap<>();
        fraudEvent.put("transactionId",transaction.getId());
        fraudEvent.put("senderAccountNumber",transaction.getSenderAccountNumber());

        fraudEvent.put("reason",reason);

        kafkaTemplate.send(FRAUD_DETECTED_TOPIC,transaction.getSenderAccountNumber(),fraudEvent);
        log.warn("fraud.detected published-account:{} will be blocked,kindly contact to bank ",
                transaction.getSenderAccountNumber());

        //SAGA COMPENSATION- refund sender
        compensateTransaction(transaction,reason);


    }

    private void compensateTransaction(Transaction transaction, String reason) {
        log.warn("SAGA COMPENSATION-refunding:{} amount:{}",
                transaction.getSenderAccountNumber(),
                transaction.getAmount());

        //credit money back to sender
        accountServiceClient.creditBalance(transaction.getSenderAccountNumber(),
                transaction.getAmount());
        transaction.setStatus(TransactionStatus.FLAGGED);
        transaction.setFailureReason(reason+"-SAGA Compensation executed,amount refunded at "+ LocalDateTime.now());

        transactionRepository.save(transaction);

        //publish refund event
        Map<String,Object> refundEvent=new HashMap<>();
        refundEvent.put("transactionId",transaction.getId());
        refundEvent.put("senderAccountNumber",transaction.getSenderAccountNumber());
        refundEvent.put("amount",transaction.getAmount());
        refundEvent.put("reason",reason);

        kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC,transaction.getId(),refundEvent);

        log.info("SAGA COMPENSATION COMPLETE-{} refunded to {}",
                transaction.getAmount(),transaction.getSenderAccountNumber());
    }

    public void processCleanResult(String transactionId) {
        Transaction transaction=transactionRepository.findById(transactionId)
                .orElseThrow(()->new RuntimeException(
                        "Transaction not found"+transactionId
                ));

        if(transaction.getStatus()!= TransactionStatus.PROCESSING){
            log.warn("Transaction{} not PROCESSING-skipping",transactionId);
            return;
        }

        completeTransaction(transaction);

    }
}
