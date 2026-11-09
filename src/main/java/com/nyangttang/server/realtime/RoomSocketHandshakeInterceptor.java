package com.nyangttang.server.realtime;

import org.springframework.http.*;
import org.springframework.http.server.*;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;
import java.util.regex.Pattern;

public class RoomSocketHandshakeInterceptor implements HandshakeInterceptor {
    private static final Pattern PATH = Pattern.compile("/ws/rooms/([1-9][0-9]{0,18})");
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        var match = PATH.matcher(request.getURI().getPath());
        if (request.getURI().getRawQuery() != null || !match.matches()) {
            response.setStatusCode(HttpStatus.BAD_REQUEST); return false;
        }
        try { attributes.put("roomId", Long.parseLong(match.group(1))); }
        catch (NumberFormatException invalid) { response.setStatusCode(HttpStatus.BAD_REQUEST); return false; }
        return true;
    }
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {}
}
