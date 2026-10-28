package com.supportdesk.auth.config;

import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.security.JwtAuthenticationFilter;
import com.supportdesk.auth.security.Permissions;
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

@Configuration                                        // Spring: this class creates beans at startup
@EnableWebSecurity                                    // Spring Security: use OUR SecurityFilterChain below
@EnableMethodSecurity                                 // Spring Security: makes @PreAuthorize on controllers work
@EnableConfigurationProperties(AuthProperties.class)  // Spring Boot: create AuthProperties from application.yml
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean                          // Spring: the rule set Spring Security uses for every request
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   UserRepository userRepository,
                                                   Clock clock) throws Exception {
        // TODO (after Member 3 merges): delete this line and add "SecurityErrorWriter errorWriter"
        //      as a parameter of this method, so Spring injects the @Component that also audits ACCESS_DENIED.
        SecurityErrorWriter errorWriter = new SecurityErrorWriter();

        http
                .csrf(AbstractHttpConfigurer::disable)        // tokens in a header, not cookies → no CSRF
                .cors(Customizer.withDefaults())              // use corsConfigurationSource() below
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // 1. Open to everyone
                    auth.requestMatchers(HttpMethod.POST, Permissions.PUBLIC_POST.toArray(String[]::new)).permitAll();
                    auth.requestMatchers(Permissions.PUBLIC_ANY.toArray(String[]::new)).permitAll();

                    // 2. One rule per row of the permission matrix (E1-S2)
                    for (Permissions.Rule rule : Permissions.RULES) {
                        auth.requestMatchers(rule.pattern()).hasAnyRole(rule.roleNames());   // else 403
                    }

                    // 3. Everything else: any logged-in user (e.g. /api/auth/me, /api/auth/logout)
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorWriter)   // 401 as JSON
                        .accessDeniedHandler(errorWriter))       // 403 as JSON
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository, clock),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${supportdesk.cors.allowed-origins}") List<String> allowedOrigins) {
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