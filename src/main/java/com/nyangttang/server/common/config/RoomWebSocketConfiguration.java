
package com.nyangttang.server.common.config;

import com.nyangttang.server.realtime.RoomSocketHandler;
import com.nyangttang.server.realtime.RoomSocketHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class RoomWebSocketConfiguration implements WebSocketConfigurer {
    private final RoomSocketHandler handler;
    private final String[] origins;
    public RoomWebSocketConfiguration(RoomSocketHandler handler,
            @Value("${FRONTEND_ORIGINS:http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000}") String origins) {
        this.handler = handler; this.origins = CorsWebConfiguration.parseOrigins(origins);
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/rooms/*")
                .addInterceptors(new RoomSocketHandshakeInterceptor())
                .setAllowedOrigins(origins);
    }
}
