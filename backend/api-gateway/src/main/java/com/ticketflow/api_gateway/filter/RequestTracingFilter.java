package com.ticketflow.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Donne à chaque requête un identifiant unique (X-Request-Id) qui la suit jusqu'aux microservices,
 * et journalise sa durée. Appliqué à TOUTES les routes.
 */
@Component
public class RequestTracingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestTracingFilter.class);
    static final String REQUEST_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. L'identifiant : celui reçu s'il existe, sinon un nouveau
        String received = exchange.getRequest().getHeaders().getFirst(REQUEST_ID);
        String requestId = (received != null && !received.isBlank()) ? received : UUID.randomUUID().toString();

        // 2. Les objets du réactif sont immuables : on crée une COPIE de la requête avec l'en-tête en plus
        ServerHttpRequest request = exchange.getRequest().mutate().header(REQUEST_ID, requestId).build();
        ServerWebExchange tracedExchange = exchange.mutate().request(request).build();

        // 3. Ajouté à la réponse juste avant son envoi (après, les en-têtes ne sont plus modifiables)
        tracedExchange.getResponse().beforeCommit(() -> {
            tracedExchange.getResponse().getHeaders().set(REQUEST_ID, requestId);
            return Mono.empty();
        });

        long start = System.nanoTime();

        // 4. On passe la main à la suite de la chaîne. AUCUNE attente ici : on décrit seulement
        //    ce qu'il faudra faire quand ce sera fini (doFinally).
        return chain.filter(tracedExchange)
                .doFinally(signal -> log.info("{} {} → {} en {} ms [{}]",
                        request.getMethod(),
                        request.getPath(),
                        tracedExchange.getResponse().getStatusCode(),
                        (System.nanoTime() - start) / 1_000_000,
                        requestId));
    }

    // Exécuté en premier : l'identifiant existe avant tous les autres filtres
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}