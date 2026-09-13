package com.checkout.payment.gateway.model.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.model.validation.FutureExpiryDateValidator.FutureExpiryDateValidatorLogic;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FutureExpiryDateValidatorTest {

  @Test
  void shouldReturnValidIfYearInTheFuture() {
    assertTrue(
        FutureExpiryDateValidatorLogic.isValid(1, 2027, Instant.parse("2026-06-30T23:59:59Z")));
  }

  @Test
  void shouldReturnValidIfExpiresCurrentMonth() {
    assertTrue(
        FutureExpiryDateValidatorLogic.isValid(6, 2026, Instant.parse("2026-06-30T23:59:59Z")));
  }

  @Test
  void shouldReturnInvalidIfExpired() {
    assertFalse(
        FutureExpiryDateValidatorLogic.isValid(5, 2026, Instant.parse("2026-06-01T00:00:00Z")));
  }
}