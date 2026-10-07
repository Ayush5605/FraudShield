package com.bank.transactionservice.Repository;

import com.bank.transactionservice.DTO.TransactionResponse;
import com.bank.transactionservice.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,String> {

      List<Transaction> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
