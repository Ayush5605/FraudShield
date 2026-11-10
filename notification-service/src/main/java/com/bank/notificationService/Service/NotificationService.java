
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
}
