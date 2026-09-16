package com.bank.payment.orchestration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/** Published to partner.{type}.request. correlationId embeds the target workflow id. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerRequest implements Serializable {
    private String correlationId;
    private String uetr;
    private PartnerType partnerType;
    private String payload;
    private Instant sentAt;
}
