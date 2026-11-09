package com.nyangttang.server.realtime;

import com.nyangttang.server.room.dto.RoomSnapshotResponse;
import com.nyangttang.server.room.service.RoomSnapshotService;
import com.nyangttang.server.user.service.AuthService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.*;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.LongSupplier;

@Component
public class RoomSocketHandler extends TextWebSocketHandler {
    static final int MAX_CONNECTIONS = 64, MAX_PER_USER = 4;
    private static final long AUTH_TIMEOUT = TimeUnit.SECONDS.toNanos(5);
    private static final long HEARTBEAT_TIMEOUT = TimeUnit.SECONDS.toNanos(45);
    private final AuthService auth;
    private final RoomSnapshotService snapshots;
    private final JsonMapper json;
    private final LongSupplier nanoTime;
    private final ConcurrentMap<String, Connection> connections = new ConcurrentHashMap<>();
    private final Object admission = new Object();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(daemon("room-ws-timer"));
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64), daemon("room-ws-worker"), new ThreadPoolExecutor.AbortPolicy());
    private volatile boolean stopping;

    @Autowired
    public RoomSocketHandler(AuthService auth, RoomSnapshotService snapshots, JsonMapper json) {
        this(auth, snapshots, json, System::nanoTime);
    }
    RoomSocketHandler(AuthService auth, RoomSnapshotService snapshots, JsonMapper json, LongSupplier nanoTime) {
        this.auth = auth; this.snapshots = snapshots; this.json = json; this.nanoTime = nanoTime;
    }
    private static ThreadFactory daemon(String prefix) {
        var counter = new AtomicInteger();
        return task -> {var thread = new Thread(task, prefix + "-" + counter.incrementAndGet()); thread.setDaemon(true); return thread;};
    }
    @PostConstruct
    public void start() { timer.scheduleWithFixedDelay(this::tick, 1, 1, TimeUnit.SECONDS); }

    @Override
    public void afterConnectionEstablished(WebSocketSession raw) throws Exception {
        if (!(raw.getAttributes().get("roomId") instanceof Long roomId) || roomId < 1) {
            raw.close(new CloseStatus(4400, "INVALID_ROOM")); return;
        }
        raw.setTextMessageSizeLimit(2048);
        raw.setBinaryMessageSizeLimit(2048);
        var connection = new Connection(new ConcurrentWebSocketSessionDecorator(raw, 2000, 256 * 1024), roomId, nanoTime.getAsLong());
        synchronized (admission) {
            if (stopping || connections.size() >= MAX_CONNECTIONS) {
                raw.close(new CloseStatus(4429, "CONNECTION_LIMIT")); return;
            }
            connections.put(raw.getId(), connection);
        }
        try { send(connection, Map.of("type", "AUTH_REQUIRED", "authTimeoutSeconds", 5)); }
        catch (IOException failure) { close(connection, 1011, "SEND_FAILED"); }
    }
    @Override
    protected void handleTextMessage(WebSocketSession raw, TextMessage message) {
        var c = connections.get(raw.getId()); if (c == null) return;
        synchronized (c) {
            if (!active(c)) return;
            long now = nanoTime.getAsLong();
            if (expired(c, now)) { close(c, 4408, "TIMEOUT"); return; }
            if (message.getPayloadLength() > 2048) { close(c, 1009, "MESSAGE_TOO_BIG"); return; }
            if (now - c.messageWindow >= TimeUnit.SECONDS.toNanos(10)) { c.messageWindow = now; c.messageCount = 0; }
            if (++c.messageCount > 30) { close(c, 4429, "MESSAGE_LIMIT"); return; }
            try {
                var payload = json.readTree(message.getPayload());
                if (payload == null || !payload.isObject() || !payload.path("type").isString()) {
                    close(c, 4400, "INVALID_MESSAGE"); return;
                }
                String type = payload.path("type").asString();
                if (c.userId == null) {
                    if (!"AUTH".equals(type) || !payload.path("accessToken").isString()) {
                        close(c, 4401, "AUTH_REQUIRED"); return;
                    }
                    String token = payload.path("accessToken").asString();
                    if (!token.matches("[A-Za-z0-9_-]{43}")) { close(c, 4401, "UNAUTHORIZED"); return; }
                    Long userId = auth.getMe("Bearer " + token).id();
                    RoomSnapshotResponse snapshot = snapshots.getSnapshot(c.roomId, userId);
                    if (nanoTime.getAsLong() - c.connectedAt >= AUTH_TIMEOUT) { close(c, 4408, "AUTH_TIMEOUT"); return; }
                    synchronized (admission) {
                        if (connections.values().stream().filter(x -> userId.equals(x.userId)).count() >= MAX_PER_USER) {
                            close(c, 4429, "USER_CONNECTION_LIMIT"); return;
                        }
                        c.token = token; c.userId = userId;
                    }
                    c.lastClientMessage = now;
                    push(c, snapshot);
                } else if ("PING".equals(type)) {
                    c.lastClientMessage = now;
                    send(c, Map.of("type", "PONG"));
                } else {
                    // No client-supplied movement, user ID or room switching is accepted.
                    close(c, 4400, "UNSUPPORTED_MESSAGE");
                }
            } catch (ResponseStatusException denied) { denied(c, denied); }
            catch (tools.jackson.core.JacksonException malformed) { close(c, 4400, "INVALID_JSON"); }
            catch (Exception failure) { close(c, 1011, "SERVER_ERROR"); }
        }
    }
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        var c = connections.get(session.getId()); if (c != null) close(c, 1003, "TEXT_ONLY");
    }
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        var c = connections.get(session.getId()); if (c != null) close(c, 1011, "TRANSPORT_ERROR");
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        var c = connections.remove(session.getId()); if (c != null) c.token = null;
    }

    void tick() {
        if (stopping) return;
        long now = nanoTime.getAsLong();
        for (var c : connections.values()) {
            if (!c.session.isOpen()) { connections.remove(c.session.getId(), c); c.token = null; continue; }
            if (expired(c, now)) { close(c, 4408, "TIMEOUT"); continue; }
            if (c.userId == null || !c.pending.compareAndSet(false, true)) continue;
            try {
                workers.execute(() -> { try { refresh(c); } finally { c.pending.set(false); } });
            } catch (RejectedExecutionException full) { c.pending.set(false); close(c, 1013, "TRY_AGAIN_LATER"); }
        }
    }
    void refresh(Connection c) {
        synchronized (c) {
            if (!active(c) || c.userId == null) return;
            try {
                // Recheck session validity and current room membership on every refresh.
                var me = auth.getMe("Bearer " + c.token);
                if (!me.id().equals(c.userId)) { close(c, 4401, "UNAUTHORIZED"); return; }
                var snapshot = snapshots.getSnapshot(c.roomId, c.userId);
                if (!snapshot.equals(c.lastSnapshot)) push(c, snapshot);
            } catch (ResponseStatusException denied) { denied(c, denied); }
            catch (Exception failure) { close(c, 1011, "SERVER_ERROR"); }
        }
    }
    private boolean expired(Connection c, long now) {
        return c.userId == null ? now - c.connectedAt >= AUTH_TIMEOUT : now - c.lastClientMessage >= HEARTBEAT_TIMEOUT;
    }
    private boolean active(Connection c) { return !stopping && connections.get(c.session.getId()) == c && c.session.isOpen(); }
    private void push(Connection c, RoomSnapshotResponse snapshot) throws IOException {
        if (!active(c)) return;
        send(c, Map.of("type", "SNAPSHOT", "streamSeq", ++c.streamSeq, "data", snapshot));
        c.lastSnapshot = snapshot;
    }
    private void send(Connection c, Object body) throws IOException {
        c.session.sendMessage(new TextMessage(json.writeValueAsString(body)));
    }
    private void denied(Connection c, ResponseStatusException failure) {
        int code = failure.getStatusCode().value();
        close(c, code == 401 ? 4401 : code == 403 ? 4403 : code == 404 ? 4404 : 1011,
                code == 401 ? "UNAUTHORIZED" : code == 403 ? "FORBIDDEN" : code == 404 ? "ROOM_NOT_FOUND" : "SERVER_ERROR");
    }
    private void close(Connection c, int code, String reason) {
        connections.remove(c.session.getId(), c); c.token = null;
        try { c.session.close(new CloseStatus(code, reason)); } catch (IOException ignored) { /* already disconnected */ }
    }
    @PreDestroy
    public void stop() {
        stopping = true; timer.shutdownNow(); workers.shutdownNow();
        for (var c : connections.values()) close(c, 1001, "SERVER_SHUTDOWN");
    }
    // Package-private for protocol tests; never exposed over HTTP.
    Connection connection(String id) { return connections.get(id); }
    int connectionCount() { return connections.size(); }
    static final class Connection {
        final WebSocketSession session; final Long roomId; final long connectedAt;
        final AtomicBoolean pending = new AtomicBoolean();
        volatile Long userId; volatile String token; volatile long lastClientMessage;
        long streamSeq; RoomSnapshotResponse lastSnapshot; long messageWindow; int messageCount;
        Connection(WebSocketSession session, Long roomId, long now) {
            this.session = session; this.roomId = roomId; this.connectedAt = now;
            this.lastClientMessage = now; this.messageWindow = now;
        }
    }
}
