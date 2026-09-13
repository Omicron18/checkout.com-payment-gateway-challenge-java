package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.model.validation.FutureExpiryDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.hibernate.validator.constraints.Range;

@FutureExpiryDate
public record PaymentRequest(

    @JsonProperty("card_number")
    @Size(min = 14, max = 19)
    @Pattern(regexp = "^\\d+$", message = "must be numeric")
    String cardNumber,

    @JsonProperty("expiry_month")
    @Min(1)
    @Max(12)
    @Digits(integer = 2, fraction = 0)
    int expiryMonth,

    @JsonProperty("expiry_year")
    @Range(min = 2000, max = 2099)
    int expiryYear,

    @Pattern(regexp = "^(USD|GBP|EUR)$", message = "must be one of USD, GBP, EUR")
    String currency,

    @Digits(integer = 9, fraction = 0)
    @Positive
    BigDecimal amount,

    @Size(min = 3, max = 4)
    @Pattern(regexp = "^\\d+$", message = "must be numeric")
    String cvv
) {
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }
}