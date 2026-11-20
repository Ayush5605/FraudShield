
package com.bank.notificationService.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    @KafkaListener(
            topics = "transaction.otp.generated",
            groupId = "notification-service-group"
    )
    public void consumeOtpGenerated(
            @Payload Map<String, Object> payload) {

        try {
            String accountNumber =
                    (String) payload.get("accountNumber");

            String otp =
                    (String) payload.get("otp");

            String transactionId =
                    (String) payload.get("transactionId");

            String amount =
                    String.valueOf(payload.get("amount"));

            String reason =
                    (String) payload.get("reason");

            String message = String.format(
                    "Suspicious activity detected on your account. " +
                            "Reason: %s. " +
                            "Transaction ID: %s. " +
                            "A transaction of %s is pending verification. " +
                            "Your OTP is: %s, valid for 5 minutes. " +
                            "If this wasn't you, please contact support.",
                    reason,
                    transactionId,
                    amount,
                    otp
            );

            sendAlert(
                    accountNumber,
                    "TRANSACTION_VERIFICATION_REQUIRED",
                    message
            );

        } catch (Exception e) {
            log.error("Error sending OTP notification", e);
            throw e;
        }
    }

    private void sendAlert(
            String accountNumber,
            String subject,
            String message) {

        // Implement SMS / Email notification logic here.

        log.info(
                "Notification triggered for account: {}, subject: {}",
                accountNumber,
                subject
        );
    }

    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(@Payload Map<String, Object> payload) {
        try {
            String senderAccount=(String)payload.get("senderAccountNumber");
            String receiverAccount=(String)payload.get("receiverAccountNumber");
            String amount=payload.get("amount").toString();

            //Debit alert
            sendAlert(senderAccount,
                    "DEBIT ALERT",
                            String.format(
                                    "%s debited from account %s",
                                    amount,senderAccount
                            )

            );

            //credit alert

            sendAlert(receiverAccount,
                    "CREDIT ALERT",
                    String.format(
                            "%s credited to account %s",
                            amount,receiverAccount
                    )

            );


        }catch(Exception e){

            log.error("Error sending transaction notfication",e.getMessage());

        }

    }

    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(@Payload Map<String, Object> payload) {

        try{

            String accountNumber=(String)payload.get("accountNumber");
            String reason=(String)payload.get("reason");

            sendAlert(accountNumber,
                    "SUSPICIOUS ACTIVITY DETECTED",
                    String.format(
                            "Your account %s has been blocked"+
                                    "Reason:%s"+
                                    "Please contact your bank immediately",
                            accountNumber,reason
                    ));




        }catch (Exception e){
            log.error("Error sending fraud alert:{}",e.getMessage());

        }


    }


    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefunded(@Payload Map<String, Object> payload) {

        try{

            String senderAccount=(String)payload.get("senderAccountNumber");
            String amount=payload.get("amount").toString();
            String reason=(String)payload.get("reason");

            sendAlert(senderAccount,
                    "REFUND PROCESSED ",
                    String.format(
                            "Your transaction of %s was cancelled.%n"+
                                    "Reason: %s%n"+
                                    "%s has been refunded to account no %s",
                            amount,reason,amount,senderAccount

                    ));

        }catch(Exception e){
            log.error("Error sending notification error",e.getMessage());

        }

    }


}
