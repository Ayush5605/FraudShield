package com.bank.transactionservice.Service;

import com.bank.transactionservice.Entity.Transaction;
import com.bank.transactionservice.Entity.TransactionStatus;
import com.bank.transactionservice.Repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String,String> redisTemplate;
    private static final long OTP_EXPIRY_MINUTES=5;


    private final KafkaTemplate<String,Object> kafkaTemplate;
    private static final String TRANSACTION_OTP_GENERATED_TOPIC="transaction.otp.generated";


    public void consumeVerificationRequired(@Payload Map<String,Object> payload){

        try{

            String transactionId=(String)payload.get("transactionId");
            String accountNumber=(String)payload.get("accountNumber");
            String reason=(String)payload.get("reason");

            log.info("verificaion required - transaction:{} reason:{}",
                    transactionId,reason);

            Transaction transaction=transactionRepository.findById(transactionId)
                    .orElseThrow(()->new RuntimeException(
                            "Transaction not found"+transactionId
                    ));

            if(transaction.getStatus()!= TransactionStatus.PROCESSING){
                log.warn("Transaction{} not PROCESSING-skipping",transactionId);
            }

            String otp=String.format("%06d",(int)(Math.random()*900000)+100000);

            //store otp in redis - expires in 5 minutes

            String otpKey="verification:otp"+transactionId;
            redisTemplate.opsForValue().set(otpKey,otp,OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepository.save(transaction);

            log.info("OTP generated for transaction:{} expires in {} min",
                    transactionId,OTP_EXPIRY_MINUTES);


        // notify user
            Map<String,Object> otpEvent=new HashMap<>();
            otpEvent.put("transactionId",transactionId);
            otpEvent.put("accountNumber",accountNumber);
            otpEvent.put("reason",reason);
            otpEvent.put("otp",otp);
            otpEvent.put("amount",payload.get("amount"));

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC,transactionId,otpEvent);

            



        }catch(Exception e){
            log.error("Error handling verification required:{}",e.getMessage());

        }

    }
}
