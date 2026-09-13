package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.AcquiringBankPaymentRequest;
import com.checkout.payment.gateway.model.AcquiringBankPaymentResponse;
import com.checkout.payment.gateway.model.PaymentRequest;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBankClient acquiringBankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      AcquiringBankClient acquiringBankClient) {
    this.paymentsRepository = paymentsRepository;
    this.acquiringBankClient = acquiringBankClient;
  }

  public PaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
  }

  public PaymentResponse processPayment(PaymentRequest paymentRequest) {
    AcquiringBankPaymentRequest acquiringBankPaymentRequest = new AcquiringBankPaymentRequest(
        paymentRequest.cardNumber(),
        paymentRequest.getExpiryDate(),
        paymentRequest.currency(),
        paymentRequest.amount().intValue(),
        paymentRequest.cvv()
    );
    AcquiringBankPaymentResponse acquiringBankPaymentResponse =
        acquiringBankClient.submitPayment(acquiringBankPaymentRequest);

    PaymentResponse paymentResponse = new PaymentResponse(
        UUID.randomUUID(),
        acquiringBankPaymentResponse.authorized() ? PaymentStatus.AUTHORIZED
            : PaymentStatus.DECLINED,
        getLast4(paymentRequest.cardNumber()),
        paymentRequest.expiryMonth(),
        paymentRequest.expiryYear(),
        paymentRequest.currency(),
        paymentRequest.amount().intValue()
    );
    paymentsRepository.add(paymentResponse);
    LOG.debug("Created payment with ID {}", paymentResponse.id());
    return paymentResponse;
  }

  private String getLast4(String cardNumber) {
    return cardNumber.substring(cardNumber.length() - 4);
  }
}
