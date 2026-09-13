package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.model.AcquiringBankPaymentRequest;
import com.checkout.payment.gateway.model.AcquiringBankPaymentResponse;
import org.springframework.stereotype.Service;

@Service
public class AcquiringBankClient {

  private final AcquiringBankFeignClient acquiringBankFeignClient;

  public AcquiringBankClient(AcquiringBankFeignClient acquiringBankFeignClient) {
    this.acquiringBankFeignClient = acquiringBankFeignClient;
  }

  public AcquiringBankPaymentResponse submitPayment(
      AcquiringBankPaymentRequest acquiringBankPaymentRequest) {
    return acquiringBankFeignClient.submitPayment(acquiringBankPaymentRequest);
  }

}
