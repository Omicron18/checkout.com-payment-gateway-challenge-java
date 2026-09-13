package com.checkout.payment.gateway.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;

@SpringBootTest
class PaymentRequestTest {

  @Autowired
  JsonMapper jsonMapper;

  @Test
  void shouldDeserialize() {
    String json = """
        {
          "card_number": "12345678901234",
          "expiry_month": 11,
          "expiry_year": 2028,
          "currency": "GBP",
          "amount": 123,
          "cvv": "987"
        }
        """;
    PaymentRequest expected = new PaymentRequest(
        "12345678901234",
        11,
        2028,
        "GBP",
        new BigDecimal(123),
        "987");
    PaymentRequest paymentRequest = jsonMapper.readValue(json, PaymentRequest.class);
    assertEquals(expected, paymentRequest);
  }
}