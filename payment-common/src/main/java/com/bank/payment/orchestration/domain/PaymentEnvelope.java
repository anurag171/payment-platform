package com.bank.payment.orchestration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/** What lands on the Kafka normalized-payments topic after MQ ingestion. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEnvelope implements Serializable {
    private String uetr;              // generated at ingestion if the source lacked one
    private String senderReference;
    private MessageFormat sourceFormat;
    private String rawMessage;
    private String sourceQueue;
    private Instant receivedAt;
}
