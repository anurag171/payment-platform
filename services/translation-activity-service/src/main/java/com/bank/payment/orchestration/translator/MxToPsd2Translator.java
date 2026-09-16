package com.bank.payment.orchestration.translator;

import com.bank.payment.orchestration.domain.MessageFormat;
import org.springframework.stereotype.Component;

/**
 * MX inputs are typically already ISO 20022 XML (an older pain.001/pacs.008 version, or
 * a domestic variant). This implementation is a pass-through + light normalization stub:
 * it wraps the payload if it isn't already in a pacs.008 envelope. Replace the body with
 * a real XSLT/version-upgrade step (e.g. via a proper XML transformation library) once
 * you have sample messages from your actual MX sources.
 */
@Component
public class MxToPsd2Translator implements MessageTranslator {

    @Override
    public boolean supports(MessageFormat format) {
        return format == MessageFormat.MX;
    }

    @Override
    public String translateToPsd2(String rawMessage) {
        if (rawMessage != null && rawMessage.contains("pacs.008")) {
            return rawMessage;
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
                  <!-- TODO: replace with a real version-upgrade transform for this source's MX dialect -->
                  <OriginalMessage><![CDATA[%s]]></OriginalMessage>
                </Document>
                """.formatted(rawMessage == null ? "" : rawMessage);
    }
}
