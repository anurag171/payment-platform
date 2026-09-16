package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReversalActivitiesImpl implements ReversalActivities {

    private static final Logger log = LoggerFactory.getLogger(ReversalActivitiesImpl.class);

    @Override
    public void reverseFxDeal(PaymentContext context) {
        // TODO: call the real FX desk / deal-booking system's cancellation API.
        log.warn("Reversing FX deal for payment {}", context.getUetr());
    }

    @Override
    public void reverseBooking(PaymentContext context) {
        // TODO: post a real reversing ledger entry (credit the debtor account back) via
        // the core banking / ledger service. Structural placeholder only.
        log.warn("Reversing ledger booking for payment {} ({} {})",
                context.getUetr(), context.getAmount(), context.getCurrency());
    }

    @Override
    public void notifyReversal(PaymentContext context, String reason) {
        // TODO: notify the originator / ops queue / customer-communication service.
        log.warn("Payment {} reversed: {}", context.getUetr(), reason);
    }
}
