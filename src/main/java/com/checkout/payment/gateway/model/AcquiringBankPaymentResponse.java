package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AcquiringBankPaymentResponse(
    boolean authorized,
    @JsonProperty("authorization_code") String authorizationCode
) {}
