package com.bank.payment.orchestration.translator;

import com.bank.payment.orchestration.domain.MessageFormat;

public interface MessageTranslator {
    boolean supports(MessageFormat format);

    /**
     * Convert a raw MT or MX message into a canonical PSD2/ISO 20022 pacs.008-shaped
     * XML string.
     */
    String translateToPsd2(String rawMessage);
}
