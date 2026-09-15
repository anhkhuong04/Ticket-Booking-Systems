package com.lak.moviebooking.reservation.infrastructure;

import com.lak.moviebooking.common.config.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
class SeatEventsWebSocketConfiguration implements WebSocketConfigurer {

    private final SeatEventsWebSocketHandler handler;
    private final CorsProperties corsProperties;

    SeatEventsWebSocketConfiguration(SeatEventsWebSocketHandler handler, CorsProperties corsProperties) {
        this.handler = handler;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/seats")
                .setAllowedOriginPatterns(corsProperties.allowedOrigins().toArray(String[]::new));
    }
}
