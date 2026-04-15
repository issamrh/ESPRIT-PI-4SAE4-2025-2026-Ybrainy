package tn.esprit.apigateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/api/users/uploads/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/courses/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/courses/**").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/api/courses/**").permitAll()
                        .pathMatchers(HttpMethod.DELETE, "/api/courses/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/courses/files/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/courses/search/ai").permitAll()
                        .pathMatchers(HttpMethod.PATCH, "/api/courses/*/publish").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/courses/{courseId}/lessons/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/courses/*/lessons/*/complete").permitAll()
                        .pathMatchers(HttpMethod.PATCH, "/api/courses/*/lessons/*/time").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/certificates/student/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/students/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/instructor/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/users/internal/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/ml/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/quizzes/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/quizzes/**").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/api/quizzes/**").permitAll()
                        .pathMatchers(HttpMethod.DELETE, "/api/quizzes/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/payments/config").permitAll()
                        .anyExchange().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.mode(XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN))
                )
                .oauth2ResourceServer(oauth -> oauth
                    .jwt(jwt -> {})
                    .authenticationEntryPoint((exchange, ex) -> reactor.core.publisher.Mono.empty())
                )
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${keycloak.auth.client-id:${KEYCLOAK_AUTH_CLIENT_ID:angular-client}}") String clientId) {
        String jwkSetUri = issuerUri + "/protocol/openid-connect/certs";
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> clientValidator = new JwtClientClaimValidator(clientId);
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, clientValidator));

        return jwtDecoder;
    }
}
