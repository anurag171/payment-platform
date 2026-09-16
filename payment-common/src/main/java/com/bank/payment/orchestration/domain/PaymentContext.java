package com.bank.payment.orchestration.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * The object threaded through every workflow in the chain (Inbound -> Qualification ->
 * [Warehouse ->] Mop -> [Repair ->] Processing). Each stage enriches this and hands it
 * to the next child workflow. It is also what gets persisted into {@link Payment} for
 * audit after each stage via AuditActivities.
 *
 * Kept as a plain, Jackson-friendly POJO (no interfaces, no generics) since it crosses
 * Temporal's data converter on every child-workflow boundary.
 */
@Data
public class PaymentContext implements Serializable {

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

    private PaymentStatus status = PaymentStatus.RECEIVED;
    private int repairAttempts;
    private String failureReason;

    private Set<PartnerType> requiredPartners = new HashSet<>();
}
