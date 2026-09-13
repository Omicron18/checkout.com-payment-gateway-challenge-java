package com.checkout.payment.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import java.time.Clock;

@SpringBootApplication
@EnableFeignClients
public class PaymentGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentGatewayApplication.class, args);
  }

  @Bean
  public Clock getClock() {
    return Clock.systemUTC();
  }
}
