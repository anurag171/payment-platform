package com.bank.payment.orchestration.activities;

import com.bank.payment.orchestration.domain.AccountData;
import com.bank.payment.orchestration.domain.CustomerData;
import com.bank.payment.orchestration.domain.PaymentContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls out to Customer Service / Account Service. Wired with RestClient placeholders —
 * point restClient at your internal service mesh / API gateway URL (respecting the
 * "internal API tunnels only" constraint: no public internet calls here).
 */
@Component
public class EnrichmentActivitiesImpl implements EnrichmentActivities {

    private final RestClient customerServiceClient;
    private final RestClient accountServiceClient;

    public EnrichmentActivitiesImpl(RestClient.Builder restClientBuilder) {
        // TODO: point these base URLs at your internal customer/account service endpoints
        // (env-configurable, reachable only via your internal API tunnel).
        this.customerServiceClient = restClientBuilder.baseUrl("http://customer-service.internal").build();
        this.accountServiceClient = restClientBuilder.baseUrl("http://account-service.internal").build();
    }

    @Override
    public CustomerData enrichCustomer(PaymentContext context) {
        // TODO: replace with the real call, e.g.:
        // return customerServiceClient.get().uri("/customers/{id}", context.getDebtorAccount())
        //         .retrieve().body(CustomerData.class);
        return new CustomerData(context.getDebtorAccount(), "UNKNOWN", "PENDING_LOOKUP", "STANDARD");
    }

    @Override
    public AccountData enrichAccount(PaymentContext context) {
        // TODO: replace with the real call to accountServiceClient.
        return new AccountData(context.getDebtorAccount(), context.getDebtorAccount(),
                context.getCurrency(), true, "Assumed valid pending real account-service integration");
    }
}
