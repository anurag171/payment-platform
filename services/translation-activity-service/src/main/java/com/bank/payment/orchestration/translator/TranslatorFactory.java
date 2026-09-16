package com.bank.payment.orchestration.translator;

import com.bank.payment.orchestration.domain.MessageFormat;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TranslatorFactory {

    private final List<MessageTranslator> translators;

    public TranslatorFactory(List<MessageTranslator> translators) {
        this.translators = translators;
    }

    public MessageTranslator forFormat(MessageFormat format) {
        return translators.stream()
                .filter(t -> t.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No translator registered for " + format));
    }
}
