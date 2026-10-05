package com.bank.transactionservice.Entity;


/** Transaction lifecycle
 * PENDING->PROCESSING->COMPLETED(clean transaction)
 *                    ->PENDING_VERIFICATION(suspicion detected)
 *                    ->completed(verified)
 *                    ->flagged(block the acc & SAGA Refund)
 *
 *                    ->FAILED
 */

public enum TransactionStatus {

    PENDING,
    PROCESSING,
    PRENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED
}
