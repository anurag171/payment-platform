package com.bank.payment.orchestration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/** Consumed from partner.{type}.response. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerResponse implements Serializable {
    private String correlationId;
    private String uetr;
    private PartnerType partnerType;
    private boolean approved;
    private String reasonCode;
    private Instant respondedAt;
}
