package com.ticketflow.event_service.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // La vraie horloge de l'application ; les tests en injectent une figée
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
