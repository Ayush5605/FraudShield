package com.bank.payementService.DTO;

import com.bank.payementService.Entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentOrderResponse {

    private String paymentId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String razorpayKeyId;

}
