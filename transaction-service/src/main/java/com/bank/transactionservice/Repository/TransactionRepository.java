package com.bank.transactionservice.Repository;

import com.bank.transactionservice.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction,String> {
}
