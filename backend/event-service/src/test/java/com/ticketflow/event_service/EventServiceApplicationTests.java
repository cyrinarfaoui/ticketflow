package com.ticketflow.event_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class EventServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
