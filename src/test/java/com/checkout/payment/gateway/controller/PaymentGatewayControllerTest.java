package com.checkout.payment.gateway.controller;


import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.wiremock.spring.EnableWireMock;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {"app.acquiring-bank-url=${wiremock.server.baseUrl}"})
@AutoConfigureMockMvc
@EnableWireMock
class PaymentGatewayControllerTest {

  private static final String BASE_URL = "/v1/payment";

  private static final String EXPECTED_REQUEST_TO_ACQUIRING_BANK = """
      {
        "card_number": "2222405343248877",
        "expiry_date": "4/2027",
        "currency":"GBP",
        "amount":100,
        "cvv":"1234"
      }
      """;

  private static final String AUTHORIZED_RESPONSE_FROM_ACQUIRING_BANK = """
      {
         "authorized": true,
         "authorization_code": "0bb07405-6d44-4b50-a14f-7ae0beff13ad"
      }
      """;

  private static final String DECLINED_RESPONSE_FROM_ACQUIRING_BANK = """
      {
         "authorized": false,
         "authorization_code": ""
      }
      """;

  private static final String PAYMENT_REQUEST = """
      {
        "card_number": "2222405343248877",
        "expiry_month": 4,
        "expiry_year": 2027,
        "currency": "GBP",
        "amount": 100,
        "cvv": "1234"
      }
      """;

  private static final String ACQUIRING_BANK_PATH = "/payments";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private PaymentsRepository paymentsRepository;

  @Autowired
  private JsonMapper jsonMapper;

  @MockitoBean
  private Clock clock;

  @BeforeEach
  void beforeAll() {
    when(clock.instant()).thenReturn(Instant.parse("2020-01-01T00:00:00Z"));
  }

  @Nested
  class Get {

    @Test
    void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
      PaymentResponse payment = new PaymentResponse(UUID.randomUUID(),
          PaymentStatus.AUTHORIZED,
          "4321",
          12,
          2024,
          "USD",
          10);

      paymentsRepository.add(payment);

      mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/" + payment.id()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value(payment.status().getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value(payment.cardNumberLastFour()))
          .andExpect(jsonPath("$.expiryMonth").value(payment.expiryMonth()))
          .andExpect(jsonPath("$.expiryYear").value(payment.expiryYear()))
          .andExpect(jsonPath("$.currency").value(payment.currency()))
          .andExpect(jsonPath("$.amount").value(payment.amount()));
    }

    @Test
    void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
      mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/" + UUID.randomUUID()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Payment not found"));
    }
  }

  @Nested
  class Post {

    @Nested
    class Valid {

      @Test
      void shouldSucceed_whenValidBodyPosted_andBankAuthorizes() throws Exception {
        stubFor(post(ACQUIRING_BANK_PATH)
            .withHeader(ContentTypeHeader.KEY, equalTo("application/json"))
            .withRequestBody(equalToJson(EXPECTED_REQUEST_TO_ACQUIRING_BANK))
            .willReturn(ok().withHeader(ContentTypeHeader.KEY, "application/json")
                .withBody(AUTHORIZED_RESPONSE_FROM_ACQUIRING_BANK)));

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(PAYMENT_REQUEST))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
            .andExpect(jsonPath("$.cardNumberLastFour").value("8877"))
            .andExpect(jsonPath("$.expiryMonth").value(4))
            .andExpect(jsonPath("$.expiryYear").value(2027))
            .andExpect(jsonPath("$.currency").value("GBP"))
            .andExpect(jsonPath("$.amount").value("100"));
      }

      @Test
      void shouldSucceed_whenValidBodyPosted_andBankDeclines() throws Exception {
        stubFor(post(ACQUIRING_BANK_PATH).withHeader(ContentTypeHeader.KEY, equalTo("application/json"))
            .withRequestBody(equalToJson(EXPECTED_REQUEST_TO_ACQUIRING_BANK))
            .willReturn(ok().withHeader(ContentTypeHeader.KEY, "application/json").withBody(
                DECLINED_RESPONSE_FROM_ACQUIRING_BANK)));

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(PAYMENT_REQUEST))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()))
            .andExpect(jsonPath("$.cardNumberLastFour").value("8877"))
            .andExpect(jsonPath("$.expiryMonth").value(4))
            .andExpect(jsonPath("$.expiryYear").value(2027))
            .andExpect(jsonPath("$.currency").value("GBP"))
            .andExpect(jsonPath("$.amount").value("100"));
      }

      @Test
      void shouldFail_whenValidBodyPosted_andBankReturns5xx() throws Exception {
        stubFor(post(ACQUIRING_BANK_PATH).withHeader(ContentTypeHeader.KEY, equalTo("application/json"))
            .withRequestBody(equalToJson(EXPECTED_REQUEST_TO_ACQUIRING_BANK))
            .willReturn(WireMock.status(500)));

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PAYMENT_REQUEST))
            .andExpect(status().is(500))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").doesNotExist())
            .andExpect(jsonPath("$.message")
                .value("Error communicating with acquiring bank"));
      }

      @Test
      void shouldFail_whenValidBodyPosted_andUnhandledExceptionIsThrown() throws Exception {
        when(clock.instant()).thenThrow(new RuntimeException());

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PAYMENT_REQUEST))
            .andExpect(status().is(500))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").doesNotExist())
            .andExpect(jsonPath("$.message")
                .value("Unknown error"));
      }
    }

    @Nested
    class Invalid {

      @Test
      void shouldReturn400_whenInvalidBodyPosted_notJson() throws Exception {
        String body = """
            not json
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.message").value("Invalid request body"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_jsonMissingRequiredFields() throws Exception {
        String body = """
            { "key": "value"}
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.message").value("Invalid request body"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_cardNumberTooShort() throws Exception {
        String body = """
            {
              "card_number": "1234567890123",
              "expiry_month": 4,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 100,
              "cvv": "1234"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.cardNumber").value("size must be between 14 and 19"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_cardNumberTooLong() throws Exception {
        String body = """
            {
              "card_number": "12345678901234567890",
              "expiry_month": 4,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 100,
              "cvv": "1234"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.cardNumber").value("size must be between 14 and 19"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_cardNumberContainsNonNumericChar() throws Exception {
        String body = """
            {
              "card_number": "a123456789012345678",
              "expiry_month": 4,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 100,
              "cvv": "1234"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.cardNumber").value("must be numeric"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_invalidMonth0() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 0,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 100,
              "cvv": "1234"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.errors.expiryMonth").value("must be greater than or equal to 1"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_invalidMonth13() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 13,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 100,
              "cvv": "1234"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.expiryMonth").value("must be less than or equal to 12"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_yearTooFarInFuture() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 4,
              "expiry_year": 2100,
              "currency": "GBP",
              "amount": 100,
              "cvv": "1234"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.expiryYear").value("must be between 2000 and 2099"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_unknownCurrency() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 4,
              "expiry_year": 2027,
              "currency": "JPY",
              "amount": 100,
              "cvv": "1234"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.currency").value("must be one of USD, GBP, EUR"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_fractionalAmount() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 4,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 0.1,
              "cvv": "1234"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.amount").value("numeric value out of bounds (<9 digits>.<0 digits> expected)"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_zeroAmount() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 4,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 0,
              "cvv": "1234"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.amount").value("must be greater than 0"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_amountTooBig() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 4,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 1234567890,
              "cvv": "1234"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.amount").value("numeric value out of bounds (<9 digits>.<0 digits> expected)"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_cvvTooShort() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 4,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 123,
              "cvv": "12"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.cvv").value("size must be between 3 and 4"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_cvvTooLong() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 4,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 123,
              "cvv": "12345"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.cvv").value("size must be between 3 and 4"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_cvvContainsInvalidChars() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 4,
              "expiry_year": 2027,
              "currency": "GBP",
              "amount": 123,
              "cvv": "a12"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.cvv").value("must be numeric"));
      }

      @Test
      void shouldReturn400_whenInvalidBodyPosted_expiryDateInPast() throws Exception {
        String body = """
            {
              "card_number": "2222405343248877",
              "expiry_month": 12,
              "expiry_year": 2019,
              "currency": "GBP",
              "amount": 123,
              "cvv": "987"
            }
            """;

        mvc.perform(
                MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().is(400))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
            .andExpect(jsonPath("$.errors.paymentRequest").value("Card expiry date must be in the future"));
      }

    }
  }

  @Nested
  class EndToEnd {

    @Test
    void postThenGet_shouldSucceed_whenBankAuthorizes() throws Exception {
      stubFor(post(ACQUIRING_BANK_PATH).withHeader(ContentTypeHeader.KEY, equalTo("application/json"))
          .withRequestBody(equalToJson(
              EXPECTED_REQUEST_TO_ACQUIRING_BANK
          )).willReturn(ok().withHeader(ContentTypeHeader.KEY, "application/json").withBody("""
                   {
                      "authorized": true,
                      "authorization_code": "0bb07405-6d44-4b50-a14f-7ae0beff13ad"
                   }
                   """)));

      MvcResult mvcResult = mvc.perform(
              MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                  .content(PAYMENT_REQUEST))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
          .andReturn();
      String content = mvcResult.getResponse().getContentAsString();
      String id = jsonMapper.readTree(content).get("id").stringValue();

      mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/" + id))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value("8877"))
          .andExpect(jsonPath("$.expiryMonth").value(4))
          .andExpect(jsonPath("$.expiryYear").value(2027))
          .andExpect(jsonPath("$.currency").value("GBP"))
          .andExpect(jsonPath("$.amount").value("100"));

    }

    @Test
    void postThenGet_shouldSucceed_whenBankDeclines() throws Exception {
      stubFor(post(ACQUIRING_BANK_PATH).withHeader(ContentTypeHeader.KEY, equalTo("application/json"))
          .withRequestBody(equalToJson(EXPECTED_REQUEST_TO_ACQUIRING_BANK))
          .willReturn(ok().withHeader(ContentTypeHeader.KEY, "application/json")
              .withBody(DECLINED_RESPONSE_FROM_ACQUIRING_BANK)));

      MvcResult mvcResult = mvc.perform(
              MockMvcRequestBuilders.post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                  .content(PAYMENT_REQUEST))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()))
          .andReturn();
      String content = mvcResult.getResponse().getContentAsString();
      String id = jsonMapper.readTree(content).get("id").stringValue();

      mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/" + id))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value("8877"))
          .andExpect(jsonPath("$.expiryMonth").value(4))
          .andExpect(jsonPath("$.expiryYear").value(2027))
          .andExpect(jsonPath("$.currency").value("GBP"))
          .andExpect(jsonPath("$.amount").value("100"));

    }
  }

}
