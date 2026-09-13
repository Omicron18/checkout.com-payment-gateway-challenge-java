package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.InvalidPaymentResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleException(PaymentNotFoundException ex) {
    LOG.error("Payment not found", ex);
    return new ResponseEntity<>(new ErrorResponse("Payment not found"), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<InvalidPaymentResponse> handle(MethodArgumentNotValidException ex) {
    LOG.debug("Validation failure: ", ex);
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
        errors.put(error.getField(), error.getDefaultMessage())
    );
    ex.getBindingResult().getGlobalErrors().forEach(error ->
        errors.put(error.getObjectName(), error.getDefaultMessage()));

    InvalidPaymentResponse invalidPaymentResponse = new InvalidPaymentResponse(
        PaymentStatus.REJECTED,
        errors
    );
    return new ResponseEntity<>(invalidPaymentResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handle(HttpMessageNotReadableException ex) {
    String message = "Invalid request body";
    LOG.debug(message, ex);
    return new ResponseEntity<>(new ErrorResponse(message), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(FeignException.class)
  public ResponseEntity<ErrorResponse> handle(FeignException ex) {
    String message = "Error communicating with acquiring bank";
    LOG.error(message, ex);

    ErrorResponse errorResponse = new ErrorResponse(message);
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handle(RuntimeException ex) {
    String message = "Unknown error";
    LOG.error(message, ex);

    ErrorResponse errorResponse = new ErrorResponse(message);
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

}
