package com.supportdesk.auth.security;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.List;


public class JwtAuthenticationFilter extends OncePerRequestFilter
{

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final Clock clock;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, Clock clock) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(BEARER_PREFIX.length()).trim();

            jwtService.validate(token)
                    .flatMap(verified -> userRepository.findByUsernameIgnoreCase(verified.username())
                            // A token issued before the user's last logout is revoked.
                            .filter(user -> user.getTokenVersion() == verified.tokenVersion()))
                    // A disabled or locked user is kicked out even if their token is still valid.
                    .filter(User::isActive)
                    .filter(user -> !user.isLocked(clock.instant()))
                    .ifPresent(user -> authenticate(user, request));
        }

        // Always continue. If nobody was authenticated, protected endpoints will answer 401.
        filterChain.doFilter(request, response);
    }

    private void authenticate(User user, HttpServletRequest request) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var authentication = new UsernamePasswordAuthenticationToken(
                AuthenticatedUser.from(user), null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
