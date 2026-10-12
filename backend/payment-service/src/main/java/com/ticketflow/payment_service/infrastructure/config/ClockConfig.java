package com.ticketflow.payment_service.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // L'heure "réelle" en production ; les tests injectent une horloge figée
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
