package com.bank.transactionservice.Service;

import com.bank.transactionservice.Entity.Transaction;
import com.bank.transactionservice.Entity.TransactionStatus;
import com.bank.transactionservice.Repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

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




        }catch(Exception e){

        }

    }
}
