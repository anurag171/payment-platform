package com.bank.payment.orchestration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerData implements Serializable {
    private String customerId;
    private String customerName;
    private String kycStatus;
    private String customerSegment;
}
