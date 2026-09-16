package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentEnvelope;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TranslationActivities {

    @ActivityMethod
    String translateToPsd2(PaymentEnvelope envelope);

    @ActivityMethod
    void validateCanonical(String canonicalMessage);
}
