package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AcquiringBankPaymentRequest(
    @JsonProperty("card_number") String cardNumber,
    @JsonProperty("expiry_date") String expiryDate,
    String currency,
    int amount,
    String cvv
) {}
