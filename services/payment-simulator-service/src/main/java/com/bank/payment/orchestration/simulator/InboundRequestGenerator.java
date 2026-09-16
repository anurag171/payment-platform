package com.bank.payment.orchestration.simulator;

import com.bank.payment.orchestration.domain.MessageFormat;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Publishes synthetic MT/MX payments onto the REAL inbound queues (RabbitMQ for MX,
 * ActiveMQ for MT) so the full ingestion bridge (InboundMqListener -> Kafka ->
 * NormalizedPaymentConsumer -> InboundNormalizationWorkflow) is exercised end-to-end,
 * rather than shortcutting straight to Kafka. Stands in for Rapidoid/Aeromock — see
 * README for why those weren't used directly.
 */
@Component
public class InboundRequestGenerator {

    private final RabbitTemplate rabbitTemplate;
    private final JmsTemplate jmsTemplate;
    private final String rabbitQueue;
    private final String activeMqQueue;

    public InboundRequestGenerator(RabbitTemplate rabbitTemplate, JmsTemplate jmsTemplate,
                                    @Value("${payment-orchestration.rabbit.inbound-queue}") String rabbitQueue,
                                    @Value("${payment-orchestration.activemq.inbound-queue}") String activeMqQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.jmsTemplate = jmsTemplate;
        this.rabbitQueue = rabbitQueue;
        this.activeMqQueue = activeMqQueue;
    }

    public String generateAndPublish(MessageFormat format) {
        if (format == MessageFormat.MT) {
            String mt103 = sampleMt103();
            jmsTemplate.convertAndSend(activeMqQueue, mt103);
            return mt103;
        }
        String mx = sampleMxPacs008();
        rabbitTemplate.convertAndSend(rabbitQueue, mx);
        return mx;
    }

    private String sampleMt103() {
        String ref = "SIM" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String amount = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 100000));
        return """
                :20:%s
                :23B:CRED
                :32A:%sEUR%s,00
                :50K:/12345678
                ACME CORP LTD
                :59:/87654321
                BENEFICIARY CO
                :70:INVOICE %s
                :71A:SHA
                """.formatted(ref, java.time.LocalDate.now().toString().replace("-", "").substring(2), amount, ref);
    }

    private String sampleMxPacs008() {
        String uetr = UUID.randomUUID().toString();
        String amount = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 100000));
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
                  <FIToFICstmrCdtTrf>
                    <GrpHdr><MsgId>%s</MsgId></GrpHdr>
                    <CdtTrfTxInf>
                      <PmtId><UETR>%s</UETR></PmtId>
                      <IntrBkSttlmAmt Ccy="EUR">%s</IntrBkSttlmAmt>
                    </CdtTrfTxInf>
                  </FIToFICstmrCdtTrf>
                </Document>
                """.formatted(uetr, uetr, amount);
    }
}
