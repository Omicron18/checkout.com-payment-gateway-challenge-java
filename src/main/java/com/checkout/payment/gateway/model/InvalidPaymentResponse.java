package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import java.util.Map;

public record InvalidPaymentResponse(
    PaymentStatus status,
    Map<String, String> errors
) {}
