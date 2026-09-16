package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentEnvelope;
import com.bank.payment.orchestration.translator.TranslatorFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TranslationActivitiesImpl implements TranslationActivities {

    private final TranslatorFactory translatorFactory;

    public TranslationActivitiesImpl(TranslatorFactory translatorFactory) {
        this.translatorFactory = translatorFactory;
    }

    @Override
    public String translateToPsd2(PaymentEnvelope envelope) {
        return translatorFactory.forFormat(envelope.getSourceFormat()).translateToPsd2(envelope.getRawMessage());
    }

    @Override
    public void validateCanonical(String canonicalMessage) {
        if (!StringUtils.hasText(canonicalMessage) || !canonicalMessage.contains("Document")) {
            throw new IllegalStateException("Canonical PSD2 message failed basic structural validation");
        }
    }
}
