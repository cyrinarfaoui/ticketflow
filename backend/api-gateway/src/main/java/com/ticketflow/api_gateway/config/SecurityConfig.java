package com.ticketflow.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Première barrière : la gateway vérifie que le jeton est AUTHENTIQUE (signé par Keycloak, non expiré).
     * Les services gardent les règles fines (rôle ADMIN, propriétaire d'une réservation…) : défense en profondeur.
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)       // API sans session ni cookie : pas de CSRF
                .cors(Customizer.withDefaults())                  // utilise le bean corsConfigurationSource ci-dessous
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()          // pré-requêtes CORS du navigateur
                        .pathMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()   // sondes + Prometheus
                        .pathMatchers(HttpMethod.GET, "/api/events/**").permitAll()   // catalogue public
                        // Flux public : aucune donnée personnelle, et EventSource ne sait pas envoyer de jeton
                        .pathMatchers(HttpMethod.GET, "/api/notifications/stream").permitAll()
                        .anyExchange().authenticated()                                // tout le reste : jeton obligatoire
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    // CORS centralisé : une seule origine autorisée, définie à un seul endroit
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}