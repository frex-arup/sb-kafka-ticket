package com.ticketbooking.payment.gateway;

import com.ticketbooking.payment.gateway.impl.RazorpayPaymentGateway;
import com.ticketbooking.payment.gateway.impl.StripePaymentGateway;
import com.ticketbooking.payment.model.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for obtaining the appropriate payment gateway implementation
 * based on the payment provider.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayFactory {

    private final RazorpayPaymentGateway razorpayGateway;
    private final StripePaymentGateway stripeGateway;

    /**
     * Gets the payment gateway for the specified provider.
     *
     * @param provider Payment provider enum
     * @return PaymentGateway implementation for the provider
     * @throws IllegalArgumentException if provider is unknown
     */
    public PaymentGateway getGateway(PaymentProvider provider) {
        log.debug("Getting payment gateway for provider: {}", provider);

        return switch (provider) {
            case RAZORPAY -> {
                log.info("Selected Razorpay payment gateway");
                yield razorpayGateway;
            }
            case STRIPE -> {
                log.info("Selected Stripe payment gateway (simulation mode)");
                yield stripeGateway;
            }
            case SIMULATED -> {
                log.info("Simulated payment mode (no gateway)");
                throw new IllegalArgumentException("Simulated provider should not use gateway");
            }
        };
    }
}
