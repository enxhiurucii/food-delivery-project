package com.delivery.backoffice.shared.security;

import java.util.List;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The back office is a pure OAuth2 resource server: Keycloak (realm "staff") issues the tokens,
 * this service only validates them (signature, issuer, audience, expiry) and authorizes.
 *
 * <ul>
 *   <li>{@code /api/**}: restaurant staff (roles STAFF / MANAGER / ADMIN). Tenant checks
 *       ("does this user work at this restaurant?") are done with {@code @PreAuthorize} + {@link TenantGuard}.</li>
 *   <li>{@code /internal/**}: only the webshop service account (client-credentials token with role CATALOG_READER).</li>
 *   <li>SSE streams authenticate with a short-lived one-time ticket instead of a bearer header,
 *       because the browser's EventSource cannot send headers.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // stateless bearer-token API, no cookies -> no CSRF surface
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // error pages and SSE async re-dispatches were already authorized on the original request
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/error", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/*/orders/stream").permitAll() // ticket checked in controller
                        .requestMatchers("/internal/**").hasRole(Roles.CATALOG_READER)
                        .requestMatchers("/api/me").hasAnyRole(Roles.STAFF, Roles.MANAGER, Roles.ADMIN, Roles.DRIVER)
                        .requestMatchers("/api/driver/**").hasRole(Roles.DRIVER)
                        .requestMatchers("/api/**").hasAnyRole(Roles.STAFF, Roles.MANAGER, Roles.ADMIN)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter())));
        return http.build();
    }

    /** Only needed when the SPA calls the API directly (e.g. Vite dev server). Behind nginx everything is same-origin. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/api/driver/**", config);
        return source;
    }
}
