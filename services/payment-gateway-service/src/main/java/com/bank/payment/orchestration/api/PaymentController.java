package com.bank.payment.orchestration.api;

import com.bank.payment.orchestration.domain.MessageFormat;
import com.bank.payment.orchestration.messaging.NormalizedPaymentProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final NormalizedPaymentProducer producer;

    public PaymentController(NormalizedPaymentProducer producer) {
        this.producer = producer;
    }

    public record SubmitTestPaymentRequest(MessageFormat format, String rawMessage, String senderReference) {}

    @Operation(summary = "Submit a test payment through the same path a real inbound MQ message takes")
    @PostMapping("/test")
    public ResponseEntity<Void> submitTestPayment(@RequestBody SubmitTestPaymentRequest request) {
        producer.publish(request.rawMessage(), request.format(), "api-test", request.senderReference());
        return ResponseEntity.accepted().build();
    }
}
