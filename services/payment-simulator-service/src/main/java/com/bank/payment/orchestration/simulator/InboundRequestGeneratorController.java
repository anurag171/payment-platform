package com.bank.payment.orchestration.simulator;

import com.bank.payment.orchestration.domain.MessageFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inbound request generator")
@RestController
@RequestMapping("/api/simulate/inbound")
public class InboundRequestGeneratorController {

    private final InboundRequestGenerator generator;

    public InboundRequestGeneratorController(InboundRequestGenerator generator) {
        this.generator = generator;
    }

    @Operation(summary = "Generate and publish a synthetic MT or MX payment onto the real inbound MQ")
    @PostMapping("/{format}")
    public String generate(@PathVariable MessageFormat format) {
        return generator.generateAndPublish(format);
    }
}
