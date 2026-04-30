package ru.arkhipova.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    /**
     * Validates JWT from Authorization header and populates Spring Security context.
     *
     * <p>Bad tokens (invalid signature, missing claims, malformed UUID) MUST NOT escape as 500.
     * Anything that fails parsing or validation is treated as "no auth" — the request continues
     * unauthenticated and downstream {@link org.springframework.security.web.authentication.HttpStatusEntryPoint}
     * returns 401.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getJwtFromRequest(request);

        if (StringUtils.hasText(token)) {
            try {
                if (tokenProvider.validateToken(token)) {
                    UUID userId = tokenProvider.getUserIdFromToken(token);
                    if (userId != null) {
                        UserPrincipal principal = new UserPrincipal(userId);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT authentication applied for user={}", userId);
                    }
                }
            } catch (RuntimeException ex) {
                log.debug("JWT processing failed: {}", ex.getMessage());
                // Fall through unauthenticated; the entry point will return 401.
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
