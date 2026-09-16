package com.bank.payment.orchestration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/** What an operator submits from the repair worklist UI/API. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairSubmission implements Serializable {
    private String uetr;
    private Map<String, String> correctedFields;
    private String operatorId;
    private String notes;
}
