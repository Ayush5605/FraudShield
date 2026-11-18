package com.bank.payementService.Repo;

import com.bank.payementService.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,String> {


    Optional<Payment> findByRazorpayOrderId(String orderId);
}
