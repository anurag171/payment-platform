package com.bank.payment.orchestration.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Persistent, queryable record of a payment. The Mongo _id IS the UETR — this mirrors
 * the fact that the UETR is also the root Temporal workflow id, so the same key
 * addresses the payment in both Temporal and Mongo.
 */
@Data
@Document(collection = "payments")
public class Payment {

    @Id
    private String uetr;

    private String senderReference;
    private MessageFormat sourceFormat;
    private String rawMessage;
    private String canonicalPsd2Message;

    private BigDecimal amount;
    private String currency;
    private String debtorAccount;
    private String creditorAccount;

    private CustomerData customerData;
    private AccountData accountData;

    private LocalDate valueDate;
    private boolean warehoused;

    private MopType mop;
    private ClearingRail clearingRail;

    private PaymentStatus status;
    private int repairAttempts;
    private String failureReason;

    private Instant createdAt;
    private Instant updatedAt;
}
