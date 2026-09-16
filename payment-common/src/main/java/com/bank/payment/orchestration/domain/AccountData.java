package com.bank.payment.orchestration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountData implements Serializable {
    private String accountId;
    private String iban;
    private String currency;
    private boolean valid;
    private String validationMessage;
}
