package com.ticketflow.payment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Les écouteurs Kafka ne démarrent pas pendant les tests : pas besoin d'un broker Kafka
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class PaymentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
