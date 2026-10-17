package com.ticketflow.event_service.infrastructure.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // Les règles sont lues de haut en bas : la première qui correspond s'applique.
                // Donc les plus précises en haut, les plus générales en bas.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Le bilan d'une annulation : admin seulement (AVANT la règle publique GET /events/**)
                        .requestMatchers(HttpMethod.GET, "/events/*/cancellation", "/events/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/events/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/events/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/events/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/events/*/book", "/events/*/close", "/events/*/cancel").hasRole("ADMIN")
                        // Lister toutes les réservations et les statistiques : réservé à l'admin (dashboard)
                        .requestMatchers(HttpMethod.GET, "/reservations", "/reservations/stats").hasRole("ADMIN")
                        // Réserver / annuler : tout utilisateur connecté
                        .requestMatchers("/reservations/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> realmRoles = realmAccess != null
                    ? (List<String>) realmAccess.getOrDefault("roles", List.of())
                    : List.of();

            Stream<GrantedAuthority> roleAuthorities = realmRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role));

            return Stream.concat(authorities.stream(), roleAuthorities).collect(Collectors.toList());
        });

        return converter;
    }

}