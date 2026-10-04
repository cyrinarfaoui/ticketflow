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
                "Théâtre municipal, Tunis",
                EventCategory.CONCERT,
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
                "Théâtre municipal, Tunis",
                EventCategory.CONCERT,
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
                "Théâtre municipal, Tunis",
                EventCategory.CONCERT,
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
                "Théâtre municipal, Tunis",
                EventCategory.CONCERT,
                LocalDateTime.now().plusDays(30),
                50
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("titre");
    }

    @Test
    void should_throw_exception_when_location_is_blank() {
        assertThatThrownBy(() -> new Event(
                UUID.randomUUID(),
                "Concert de jazz",
                "Description",
                "  ",
                EventCategory.CONCERT,
                LocalDateTime.now().plusDays(30),
                50
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lieu");
    }


    @Test
    void should_decrease_available_seats_when_booking() {
        Event event = new Event(
                UUID.randomUUID(),
                "Concert de jazz",
                "Description",
                "Théâtre municipal, Tunis",
                EventCategory.CONCERT,
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
                "Théâtre municipal, Tunis",
                EventCategory.CONCERT,
                LocalDateTime.now().plusDays(30),
                5
        );

        assertThatThrownBy(() -> event.book(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("places disponibles");
    }

    @Test
    void should_increase_available_seats_when_releasing() {
        Event event = new Event(
                UUID.randomUUID(),
                "Concert de jazz",
                "Description",
                "Théâtre municipal, Tunis",
                EventCategory.CONCERT,
                LocalDateTime.now().plusDays(30),
                100
        );
        event.book(10);

        event.release(4);

        assertThat(event.getAvailableSeats()).isEqualTo(94);
    }






    @Test
    void should_throw_exception_when_category_is_missing() {
        assertThatThrownBy(() -> new Event(
                UUID.randomUUID(),
                "Concert de jazz",
                "Description",
                "Théâtre municipal, Tunis",
                null,
                LocalDateTime.now().plusDays(30),
                50
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catégorie");
    }

    @Test
    void un_nouvel_evenement_est_ouvert_aux_reservations() {
        assertThat(newEvent().getStatus()).isEqualTo(EventStatus.OPEN);
    }

    @Test
    void un_evenement_cloture_refuse_les_reservations() {
        Event event = newEvent();
        event.close();

        assertThat(event.getStatus()).isEqualTo(EventStatus.CLOSED);
        assertThatThrownBy(() -> event.book(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clôturées");
    }

    @Test
    void un_evenement_cloture_accepte_encore_les_annulations() {
        Event event = newEvent();
        event.book(5);
        event.close();

        event.release(5);

        assertThat(event.getAvailableSeats()).isEqualTo(50);
    }

    @Test
    void on_ne_peut_pas_cloturer_deux_fois() {
        Event event = newEvent();
        event.close();

        assertThatThrownBy(event::close).isInstanceOf(IllegalStateException.class);
    }

    private static Event newEvent() {
        return new Event(UUID.randomUUID(), "Concert de jazz", "Description", "Tunis",
                EventCategory.CONCERT, LocalDateTime.now().plusDays(30), 50);
    }
}
