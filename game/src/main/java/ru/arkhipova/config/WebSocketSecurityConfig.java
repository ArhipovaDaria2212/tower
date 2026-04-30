package ru.arkhipova.config;

import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import ru.arkhipova.security.JwtTokenProvider;
import ru.arkhipova.security.UserPrincipal;

/**
 * Authenticates STOMP CONNECT frames and attaches the user principal to the session.
 *
 * <p>STOMP messages do not pass through the HTTP security filter chain after the WebSocket upgrade,
 * so we install a {@link ChannelInterceptor} on the inbound channel. On {@code CONNECT} we require a
 * Bearer JWT in the {@code Authorization} STOMP header; if validation fails we throw, which cleanly
 * rejects the connection. The resolved {@link UserPrincipal} is stored on the session so subsequent
 * frames are authenticated and downstream {@code @MessageMapping} handlers see a valid principal.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider tokenProvider;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtChannelInterceptor(tokenProvider));
    }

    /**
     * Validates the Bearer token on STOMP CONNECT and binds a {@link UserPrincipal} to the session.
     */
    @RequiredArgsConstructor
    @Slf4j
    static class JwtChannelInterceptor implements ChannelInterceptor {

        private final JwtTokenProvider tokenProvider;

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null) {
                return message;
            }

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String authorization = accessor.getFirstNativeHeader("Authorization");
                String token = stripBearer(authorization);

                if (!StringUtils.hasText(token) || !tokenProvider.validateToken(token)) {
                    log.warn("STOMP CONNECT rejected: missing or invalid token");
                    throw new MessagingException("Unauthorized STOMP connection");
                }

                UUID userId = tokenProvider.getUserIdFromToken(token);
                if (userId == null) {
                    log.warn("STOMP CONNECT rejected: token has no userId claim");
                    throw new MessagingException("Unauthorized STOMP connection");
                }

                UserPrincipal principal = new UserPrincipal(userId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
                accessor.setUser(authentication);
                log.debug("STOMP CONNECT authenticated for userId={}", userId);
            }
            return message;
        }

        private String stripBearer(String value) {
            if (StringUtils.hasText(value) && value.startsWith("Bearer ")) {
                return value.substring(7);
            }
            return value;
        }
    }
}
