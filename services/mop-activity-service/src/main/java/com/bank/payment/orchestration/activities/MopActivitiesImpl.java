package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.ClearingRail;
import com.bank.payment.orchestration.domain.MopType;
import com.bank.payment.orchestration.domain.PartnerType;
import com.bank.payment.orchestration.domain.PaymentContext;
import org.springframework.stereotype.Component;

/**
 * Placeholder rules engine: real MOP derivation should consult currency, corridor,
 * amount thresholds, customer instruction and cutoff times (typically an actual rules
 * engine or reference-data service). This gives a deterministic, currency-based stand-in
 * so the workflow branching is exercisable end-to-end.
 */
@Component
public class MopActivitiesImpl implements MopActivities {

    @Override
    public boolean deriveMop(PaymentContext context) {
        String currency = context.getCurrency();
        if (currency == null) {
            context.setMop(MopType.UNRESOLVED);
            return false;
        }
        MopType mop = switch (currency) {
            case "EUR" -> MopType.RTGS;
            case "GBP" -> MopType.RTGS;
            default -> MopType.CORRESPONDENT_BANKING;
        };
        context.setMop(mop);

        if (mop == MopType.RTGS) {
            context.getRequiredPartners().add(PartnerType.SANCTIONS);
            context.getRequiredPartners().add(PartnerType.FRAUD);
            context.getRequiredPartners().add(PartnerType.BOOKING);
        } else {
            context.getRequiredPartners().add(PartnerType.SANCTIONS);
            context.getRequiredPartners().add(PartnerType.FRAUD);
            context.getRequiredPartners().add(PartnerType.FX);
            context.getRequiredPartners().add(PartnerType.BOOKING);
        }
        return true;
    }

    @Override
    public void selectClearingRail(PaymentContext context) {
        String currency = context.getCurrency();
        ClearingRail rail = switch (currency == null ? "" : currency) {
            case "EUR" -> ClearingRail.TARGET2;
            case "GBP" -> ClearingRail.CHAPS;
            default -> ClearingRail.SWIFT;
        };
        context.setClearingRail(rail);
    }
}
