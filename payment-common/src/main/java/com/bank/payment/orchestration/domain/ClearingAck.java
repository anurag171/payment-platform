package com.bank.payment.orchestration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClearingAck implements Serializable {
    private String correlationId;
    private String uetr;
    private boolean settled;
    private String rejectReason;
}
