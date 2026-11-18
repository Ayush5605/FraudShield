
package com.bank.payementService.Service;

import com.bank.payementService.DTO.CreatePaymentRequest;
import com.bank.payementService.DTO.PaymentOrderResponse;
import com.bank.payementService.Entity.Payment;
import com.bank.payementService.Entity.PaymentStatus;
import com.bank.payementService.Repo.PaymentRepository;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Order;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private static final String PAYMENT_COMPLETED_TOPIC =
            "payment.completed";

    private static final String PAYMENT_FAILED_TOPIC =
            "payment.failed";


    // CREATE RAZORPAY ORDER

    public PaymentOrderResponse createPaymentOrder(
            @Valid CreatePaymentRequest request
    ) throws RazorpayException {

        log.info("Creating payment order for account: {} amount: {}",
                request.getAccountNumber(),
                request.getAmount());

        RazorpayClient razorpayClient =
                new RazorpayClient(keyId, keySecret);

        int convertedAmount = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValueExact();

        JSONObject orderRequest = new JSONObject();

        orderRequest.put("amount", convertedAmount);
        orderRequest.put("currency", "INR");

        orderRequest.put(
                "receipt",
                "rcpt_" + UUID.randomUUID()
        );

        Order razorpayOrder =
                razorpayClient.orders.create(orderRequest);

        String razorpayOrderId =
                razorpayOrder.get("id").toString();

        log.info("Razorpay order created: {}", razorpayOrderId);

        Payment payment = new Payment();

        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAccountNumber(request.getAccountNumber());
        payment.setAmount(request.getAmount());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(request.getDescription());

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentOrderResponse(
                savedPayment.getId(),
                razorpayOrderId,
                request.getAmount(),
                "INR",
                "CREATED",
                keyId
        );
    }


    // HANDLE RAZORPAY WEBHOOK

    public void handleWebHook(Map<String, Object> payload) {

        log.info("Received Razorpay Webhook: {}",
                payload.get("event"));

        String event = (String) payload.get("event");

        if ("payment.captured".equals(event)) {

            handlePaymentSuccess(payload);

        } else if ("payment.failed".equals(event)) {

            handlePaymentFailed(payload);
        }
    }


    // HANDLE SUCCESSFUL PAYMENT

    private void handlePaymentSuccess(
            Map<String, Object> payload
    ) {

        try {

            Map<String, Object> paymentData =
                    extractPaymentData(payload);

            String orderId =
                    (String) paymentData.get("order_id");

            String paymentId =
                    (String) paymentData.get("id");

            Payment payment = paymentRepository
                    .findByRazorpayOrderId(orderId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Payment not found: " + orderId
                            ));

            payment.setRazorpayPaymentId(paymentId);

            payment.setStatus(PaymentStatus.COMPLETED);

            paymentRepository.save(payment);


            // PUBLISH PAYMENT COMPLETED EVENT

            Map<String, Object> event = new HashMap<>();

            event.put("paymentId", payment.getId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("razorpayPaymentId", paymentId);

            kafkaTemplate.send(
                    PAYMENT_COMPLETED_TOPIC,
                    String.valueOf(payment.getId()),
                    event
            );

            log.info("Payment completed: {}", payment.getId());

        } catch (Exception e) {

            log.error("Error handling payment success: {}",
                    e.getMessage(), e);
        }
    }


    // HANDLE FAILED PAYMENT

    private void handlePaymentFailed(
            Map<String, Object> payload
    ) {

        try {

            Map<String, Object> paymentData =
                    extractPaymentData(payload);

            String orderId =
                    (String) paymentData.get("order_id");

            Payment payment = paymentRepository
                    .findByRazorpayOrderId(orderId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Payment not found: " + orderId
                            ));

            payment.setStatus(PaymentStatus.FAILED);

            payment.setFailureReason(
                    "Payment failed via Razorpay"
            );

            paymentRepository.save(payment);


            // PUBLISH PAYMENT FAILED EVENT

            Map<String, Object> event = new HashMap<>();

            event.put("paymentId", payment.getId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());

            event.put(
                    "reason",
                    "Payment failed via Razorpay"
            );

            kafkaTemplate.send(
                    PAYMENT_FAILED_TOPIC,
                    String.valueOf(payment.getId()),
                    event
            );

            log.warn("Payment failed: {}", payment.getId());

        } catch (Exception e) {

            log.error("Error handling payment failure: {}",
                    e.getMessage(), e);
        }
    }


    // EXTRACT PAYMENT DATA FROM WEBHOOK

    private Map<String, Object> extractPaymentData(
            Map<String, Object> payload
    ) {

        Map<String, Object> entity =
                (Map<String, Object>) payload.get("payload");

        Map<String, Object> paymentWrapper =
                (Map<String, Object>) entity.get("payment");

        return (Map<String, Object>)
                paymentWrapper.get("entity");
    }
}
