package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.PaymentContext;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Derives value date and the warehouse decision. Uses a naive weekend-only business-day
 * check as a placeholder — replace with a real currency/rail-specific holiday calendar
 * service (e.g. TARGET2 calendar for EUR, a bank-holiday service for GBP/CHAPS) before
 * relying on this for real cutoff decisions.
 */
@Component
public class ValueDateActivitiesImpl implements ValueDateActivities {

    @Override
    public PaymentContext deriveValueDate(PaymentContext context) {
        LocalDate today = LocalDate.now();
        LocalDate valueDate = nextBusinessDay(today);
        context.setValueDate(valueDate);
        context.setWarehoused(valueDate.isAfter(today));
        return context;
    }

    private LocalDate nextBusinessDay(LocalDate date) {
        LocalDate candidate = date;
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }
}
