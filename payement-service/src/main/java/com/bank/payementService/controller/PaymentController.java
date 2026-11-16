
package com.bank.payementService.controller;

import com.bank.payementService.DTO.CreatePaymentRequest;
import com.bank.payementService.DTO.PaymentOrderResponse;
import com.bank.payementService.Service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(
            @Valid @RequestBody CreatePaymentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPaymentOrder(request));
    }


    //Razorpay webhook endpoint
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebHook(
            @RequestBody Map<String, Object> payload) {

        paymentService.handleWebHook(payload);

        return ResponseEntity.ok("Webhook processed");
    }
}
