package com.checkout.payment.gateway.model.validation;

import com.checkout.payment.gateway.model.PaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class FutureExpiryDateValidator implements ConstraintValidator<FutureExpiryDate, PaymentRequest> {

  private final Clock clock;

  public FutureExpiryDateValidator(Clock clock) {
    this.clock = clock;
  }

  @Override
  public boolean isValid(PaymentRequest value, ConstraintValidatorContext context) {
    return FutureExpiryDateValidatorLogic.isValid(value.expiryMonth(), value.expiryYear(), clock.instant());
  }

  static class FutureExpiryDateValidatorLogic {
    static boolean isValid(int expiryMonth, int expiryYear, Instant now) {
      int currentMonth = now.atZone(ZoneId.of("UTC")).getMonth().getValue();
      int currentYear = now.atZone(ZoneId.of("UTC")).getYear();
      return expiryYear > currentYear ||
          (expiryYear == currentYear && expiryMonth >= currentMonth);
    }
  }
}
