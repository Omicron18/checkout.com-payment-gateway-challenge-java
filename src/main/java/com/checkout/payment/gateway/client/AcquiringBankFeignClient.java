package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.model.AcquiringBankPaymentRequest;
import com.checkout.payment.gateway.model.AcquiringBankPaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    value = "AcquiringBankClient",
    url = "${app.acquiring-bank-url}"
)
public interface AcquiringBankFeignClient {


  @RequestMapping(
      method = RequestMethod.POST,
      value = "/payments",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE
  )
  AcquiringBankPaymentResponse submitPayment(
      AcquiringBankPaymentRequest acquiringBankPaymentRequest
  );
}
