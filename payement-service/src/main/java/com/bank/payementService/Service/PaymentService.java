package com.bank.payementService.Service;

import com.bank.payementService.DTO.CreatePaymentRequest;
import com.bank.payementService.DTO.PaymentOrderResponse;
import com.bank.payementService.Repo.PaymentRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private static final String PAYMENT_COMPLETED_TPOIC="payment.completed";
    private static final String PAYMENT_FAILED_TPOIC="payment.failed";
    public void handleWebHook(Map<String, Object> payload) {
    }

    public PaymentOrderResponse createPaymentOrder(@Valid CreatePaymentRequest request) {
    }
}
