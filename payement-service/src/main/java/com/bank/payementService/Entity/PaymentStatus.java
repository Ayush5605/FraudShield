package com.bank.payementService.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter

@Entity
@Table
public enum PaymentStatus {

    CREATED,
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED

    //TODO [Reverse Engineering] generate columns from DB
}