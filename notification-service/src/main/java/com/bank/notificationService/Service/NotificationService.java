package com.bank.notificationService.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    public void consumeOtpGenerated(@Payload  Map<String, Object> payload){

        try{
            String accountNumber=(String) payload.get("accoountNumber");
            String otp=(String)payload.get("otp");
            String transactionId=(String) payload.get("transactionId");
            String amount= payload.get("amount").toString();
            String reason=(String) payload.get("reason");


            sendAlert(
                    "TRANSACTION_VERIFICATION_REQUIRED",
                    String.format(
                            "Suspicious activity detected on your account"
                    )
            );


        }catch(Exception e){

        }

    }
}
