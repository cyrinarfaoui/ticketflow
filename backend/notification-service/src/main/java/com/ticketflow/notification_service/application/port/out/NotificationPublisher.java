package com.ticketflow.notification_service.application.port.out;

import com.ticketflow.notification_service.domain.Notification;

/** Port de sortie : diffuser une notification à ceux qui écoutent (sans dire COMMENT : SSE, WebSocket, e-mail…). */
public interface NotificationPublisher {

    void publish(Notification notification);
}
