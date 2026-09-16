package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.Payment;
import com.bank.payment.orchestration.domain.PaymentContext;
import com.bank.payment.orchestration.domain.WorkflowAuditRecord;
import com.bank.payment.orchestration.repository.PaymentRepository;
import com.bank.payment.orchestration.repository.WorkflowAuditRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AuditActivitiesImpl implements AuditActivities {

    private final PaymentRepository paymentRepository;
    private final WorkflowAuditRepository auditRepository;

    public AuditActivitiesImpl(PaymentRepository paymentRepository, WorkflowAuditRepository auditRepository) {
        this.paymentRepository = paymentRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    public void recordStage(String uetr, String workflowType, String stage, PaymentContext context) {
        WorkflowAuditRecord record = new WorkflowAuditRecord(
                UUID.randomUUID().toString(), uetr, workflowType, stage,
                context.getStatus(), context.getFailureReason(), Instant.now());
        auditRepository.save(record);
        savePayment(context);
    }

    @Override
    public void savePayment(PaymentContext context) {
        Payment payment = paymentRepository.findById(context.getUetr()).orElseGet(Payment::new);
        Instant now = Instant.now();
        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(now);
        }
        BeanUtils.copyProperties(context, payment);
        payment.setUpdatedAt(now);
        paymentRepository.save(payment);
    }
}
