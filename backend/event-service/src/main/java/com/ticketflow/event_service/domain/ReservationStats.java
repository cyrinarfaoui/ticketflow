package com.ticketflow.event_service.domain;

import java.util.Map;

/**
 * Indicateurs du dashboard admin.
 * Le calcul (total, taux) est une règle métier : il vit dans le domaine, testable sans base.
 */
public record ReservationStats(long createdToday, Map<ReservationStatus, Long> byStatus) {

    public long count(ReservationStatus status) {
        return byStatus.getOrDefault(status, 0L);
    }

    public long total() {
        return byStatus.values().stream().mapToLong(Long::longValue).sum();
    }

    public double expirationRate() {
        long total = total();
        return total == 0 ? 0 : (double) count(ReservationStatus.EXPIRED) / total;
    }
}
