package com.supportdesk.auth.config;

import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.security.JwtAuthenticationFilter;
import com.supportdesk.auth.security.SecurityErrorWriter;
import com.supportdesk.auth.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean

    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }



    @Bean                          // Spring: the rule set (SecurityFilterChain) Spring Security uses for every request.
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   UserRepository userRepository,
                                                   Clock clock) throws Exception {
        SecurityErrorWriter errorWriter = new SecurityErrorWriter();

        http

                .csrf(AbstractHttpConfigurer::disable)

                .cors(Customizer.withDefaults())

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Open to everyone
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/error").permitAll()

                        .requestMatchers("/api/admin/**").hasRole("GUIDE_ADMIN")
                        .requestMatchers("/api/review/**").hasRole("REVIEWER")
                        .requestMatchers("/api/customers/**", "/api/tickets/**", "/api/queue/**").hasRole("AGENT")

                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorWriter)   // 401 as JSON
                        .accessDeniedHandler(errorWriter))       // 403 as JSON

                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository, clock),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource
    (
            @Value("${supportdesk.cors.allowed-origins}")

            List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept-Language"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}