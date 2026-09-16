package com.bank.payment.orchestration.translator;

import com.bank.payment.orchestration.domain.MessageFormat;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simplified MT103 -> pacs.008-shaped XML mapper.
 *
 * IMPORTANT: this is a reference-level field mapper covering the common MT103 tags
 * (20/23B/32A/50K/59/70/71A), not a certified ISO 20022 conversion. For real PSD2/CBPR+
 * compliance, replace this with a proper library (e.g. Prowide ISO 20022 + Prowide Core
 * for MT parsing, or your bank's MyStandards-validated translation service). Wiring is
 * kept behind {@link MessageTranslator} so swapping the implementation doesn't touch
 * any workflow or activity code.
 */
@Component
public class MtToPsd2Translator implements MessageTranslator {

    private static final Pattern TAG_PATTERN = Pattern.compile(":(\\d{2}[A-Z]?):([^\\n:]*)");

    @Override
    public boolean supports(MessageFormat format) {
        return format == MessageFormat.MT;
    }

    @Override
    public String translateToPsd2(String rawMessage) {
        Map<String, String> tags = parseTags(rawMessage);

        String msgId = tags.getOrDefault("20", "UNKNOWN");
        String valueDateAndAmount = tags.getOrDefault("32A", "");
        String ordering = tags.getOrDefault("50K", "");
        String beneficiary = tags.getOrDefault("59", "");
        String remittanceInfo = tags.getOrDefault("70", "");
        String charges = tags.getOrDefault("71A", "SHA");

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
                  <FIToFICstmrCdtTrf>
                    <GrpHdr>
                      <MsgId>%s</MsgId>
                      <SttlmInf><SttlmMtd>CLRG</SttlmMtd></SttlmInf>
                    </GrpHdr>
                    <CdtTrfTxInf>
                      <PmtId><EndToEndId>%s</EndToEndId></PmtId>
                      <IntrBkSttlmAmt>%s</IntrBkSttlmAmt>
                      <ChrgBr>%s</ChrgBr>
                      <Dbtr><Nm>%s</Nm></Dbtr>
                      <Cdtr><Nm>%s</Nm></Cdtr>
                      <RmtInf><Ustrd>%s</Ustrd></RmtInf>
                    </CdtTrfTxInf>
                  </FIToFICstmrCdtTrf>
                </Document>
                """.formatted(
                escape(msgId), escape(msgId), escape(valueDateAndAmount), escape(charges),
                escape(ordering), escape(beneficiary), escape(remittanceInfo));
    }

    private Map<String, String> parseTags(String rawMessage) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (rawMessage == null) {
            return tags;
        }
        Matcher matcher = TAG_PATTERN.matcher(rawMessage);
        while (matcher.find()) {
            tags.put(matcher.group(1), matcher.group(2).trim());
        }
        return tags;
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
