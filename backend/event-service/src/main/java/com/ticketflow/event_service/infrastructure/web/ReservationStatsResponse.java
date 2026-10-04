package com.ticketflow.event_service.infrastructure.web;

import com.ticketflow.event_service.domain.ReservationStats;
import com.ticketflow.event_service.domain.ReservationStatus;

public record ReservationStatsResponse(
        long createdToday,
        long total,
        long pending,
        long confirmed,
        long cancelled,
        long expired,
        double expirationRate
) {
    public static ReservationStatsResponse from(ReservationStats stats) {
        return new ReservationStatsResponse(
                stats.createdToday(),
                stats.total(),
                stats.count(ReservationStatus.PENDING),
                stats.count(ReservationStatus.CONFIRMED),
                stats.count(ReservationStatus.CANCELLED),
                stats.count(ReservationStatus.EXPIRED),
                stats.expirationRate()
        );
    }
}
