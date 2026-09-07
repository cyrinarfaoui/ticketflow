package com.ticketflow.event_service.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EventTest {

    @Test
    void should_create_event_with_available_seats_equal_to_total_seats() {
        Event event = new Event(
                UUID.randomUUID(),
                "Concert de jazz",
                "Une soirée jazz au théâtre municipal",
                LocalDateTime.now().plusDays(30),
                100
        );

        assertThat(event.getAvailableSeats()).isEqualTo(100);
        assertThat(event.getTotalSeats()).isEqualTo(100);
    }

    @Test
    void should_throw_exception_when_total_seats_is_zero() {
        assertThatThrownBy(() -> new Event(
                UUID.randomUUID(),
                "Concert de jazz",
                "Description",
                LocalDateTime.now().plusDays(30),
                0
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre total de places");
    }

    @Test
    void should_throw_exception_when_total_seats_is_negative() {
        assertThatThrownBy(() -> new Event(
                UUID.randomUUID(),
                "Concert de jazz",
                "Description",
                LocalDateTime.now().plusDays(30),
                -5
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_exception_when_title_is_blank() {
        assertThatThrownBy(() -> new Event(
                UUID.randomUUID(),
                "  ",
                "Description",
                LocalDateTime.now().plusDays(30),
                50
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("titre");
    }


    @Test
    void should_decrease_available_seats_when_booking() {
        Event event = new Event(
                UUID.randomUUID(),
                "Concert de jazz",
                "Description",
                LocalDateTime.now().plusDays(30),
                100
        );

        event.book(3);

        assertThat(event.getAvailableSeats()).isEqualTo(97);
    }

    @Test
    void should_throw_exception_when_booking_more_seats_than_available() {
        Event event = new Event(
                UUID.randomUUID(),
                "Concert de jazz",
                "Description",
                LocalDateTime.now().plusDays(30),
                5
        );

        assertThatThrownBy(() -> event.book(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("places disponibles");
    }





}
