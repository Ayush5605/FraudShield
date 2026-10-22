package com.bank.fraudDetectionService.Services;


import com.bank.fraudDetectionService.Client.AccountServiceClient;
import com.bank.fraudDetectionService.model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private  final RedisTemplate<String,String> redisTemplate;

    @Value("${fraud.max-transactions-per-minute}")
    private int maxTransactionsPerMinute;


    @Value("{fraud.suspicious-amount-multiplier}")
    private double suspiciousAmountMultiplier;

    private static  final String VERIFICATION_REQUIRED_TOPIC="verification.required";
    private static  final String FRAUD_CHECK_CLEAN_RESULT_TOPIC="fraud.check.clean";





    public void checkTransaction(Map<String, Object> payload) {
        String transactionId=(String)payload.get("transactionId");
        String accountNumber=(String)payload.get("senderAccountNumber");
        BigDecimal amount= new BigDecimal(payload.get("amount").toString());

        //fetch real balance from account-service

        BigDecimal senderBalance= accountServiceClient.getBalance(accountNumber);

        log.info("checking transaction :{} amount:{} balance:{}",
                transactionId,amount,senderBalance);

        FraudCheckResult result=performFraudChecks(accountNumber,amount,senderBalance);


        if(result.isFraud()){
            log.info("Suspicious activity detected account:{}"+
                    "reason:{} requesting OTP verification",
                    accountNumber,result.getReason());

            Map<String,Object> verificationEvent=new HashMap<>();
            verificationEvent.put("transaction",transactionId);
            verificationEvent.put("accountNumber",accountNumber);
            verificationEvent.put("amount",amount);
            verificationEvent.put("reason",result.getReason());

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC,transactionId,verificationEvent);

        }else{
            //transaction is clean no fraud detected

            log.info("Transaction clean");

            Map<String,Object>transactionCleanEvent=new HashMap<>();
            transactionCleanEvent.put("transactionId",transactionId);
            transactionCleanEvent.put("isFraud",false);
            transactionCleanEvent.put("reason",null);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC,transactionId,transactionCleanEvent);
        }






    }

    
    private FraudCheckResult performFraudChecks(
            String accountNumber,
            BigDecimal amount,
            BigDecimal senderBalance

    ){

        //pattern 1 velocity check
        if(isVelocityExceeded(accountNumber)){
            return new FraudCheckResult(
                    true,"Too many transactions in 60 seconds"+
                    "-velocity limit exceeded"
            );
        }

        if(isAmountSuspicious(accountNumber,amount)){
            return new FraudCheckResult(
                    true,"Unusual transaction amount "+
                    "Exceeds 3X your average"
            );

        }

        if(senderBalance.compareTo(BigDecimal.ZERO)>0
        && isBalanceCheckFailed(senderBalance,amount)){
            return new FraudCheckResult(
                    true,"Transaction exceeds 90% of account balance"
            );
        }

        return new FraudCheckResult(false,null);




    }

    private boolean isVelocityExceeded(String accountNumber){
        String key="fraud:velocity"+accountNumber;
        Long count=redisTemplate.opsForValue().increment(key);

        if(count !=null && count==1){
            redisTemplate.expire(key,60, TimeUnit.SECONDS);
        }
        log.info("velocity check -amount :{} count:{}/{}",
                accountNumber,count,maxTransactionsPerMinute);

        return count!=null && count>maxTransactionsPerMinute;

    }

    private boolean isAmountSuspicious(String accountNumber,BigDecimal amount){
        String avgKey="fraud:avg_amount"+accountNumber;
        String avgStr=redisTemplate.opsForValue().get(avgKey);

        if(avgStr==null){
            redisTemplate.opsForValue().set(avgKey,amount.toString());
            return false;
        }

        BigDecimal avgAmount=new BigDecimal(avgStr);
        BigDecimal threshold=avgAmount.multiply(
                BigDecimal.valueOf(suspiciousAmountMultiplier)
        );

        //update running avg

        BigDecimal newAvg=avgAmount.add(amount).divide(BigDecimal.valueOf(2),2, RoundingMode.HALF_UP);

        redisTemplate.opsForValue().set(avgKey,newAvg.toString());

        log.info("Amount check-amount:{} threshold:{} suspicious:{}",
                amount,threshold,amount.compareTo(threshold)>0);


        return amount.compareTo(threshold)>0;
    }
}
