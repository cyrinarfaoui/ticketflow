package com.ticketflow.api_gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTracingFilterTest {

    private final RequestTracingFilter filter = new RequestTracingFilter();

    // Une fausse "suite de chaîne" : elle mémorise ce que le filtre lui transmet, puis dit "fini"
    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    // Une fausse "suite de chaîne", PARESSEUSE comme la vraie :
    // Mono.fromRunnable n'exécute son code qu'au moment de l'abonnement, pas à l'assemblage
    private final GatewayFilterChain chain = exchange -> Mono.fromRunnable(() -> forwarded.set(exchange));

    @Test
    void genere_un_identifiant_et_le_transmet_au_service() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/events"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        String id = forwarded.get().getRequest().getHeaders().getFirst("X-Request-Id");
        assertThat(id).isNotBlank();
    }

    @Test
    void reutilise_l_identifiant_deja_present() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/events").header("X-Request-Id", "abc-123"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Request-Id")).isEqualTo("abc-123");
    }

    @Test
    void renvoie_l_identifiant_dans_la_reponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/events").header("X-Request-Id", "abc-123"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        exchange.getResponse().setComplete().block();   // dans un TEST, on a le droit de bloquer pour attendre

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Request-Id")).isEqualTo("abc-123");
    }

    @Test
    void ne_fait_rien_tant_que_personne_ne_s_abonne() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/events"));

        filter.filter(exchange, chain);   // on appelle la méthode… SANS s'abonner

        assertThat(forwarded.get()).isNull();   // rien ne s'est passé : un Mono est paresseux
    }
}