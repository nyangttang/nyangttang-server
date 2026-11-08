package com.nyangttang.server.user.service;

import com.nyangttang.server.user.dto.MeResponse;
import com.nyangttang.server.user.entity.AuthSession;
import com.nyangttang.server.user.entity.User;
import com.nyangttang.server.user.repository.AuthSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration SESSION_DURATION = Duration.ofDays(14);

    private final AuthSessionRepository authSessionRepository;

    public AuthService(AuthSessionRepository authSessionRepository) {
        this.authSessionRepository = authSessionRepository;
    }

    @Transactional
    public IssuedSession issueSession(User user) {
        String accessToken = generateToken();
        String tokenHash = hashToken(accessToken);

        Instant expiresAt = Instant.now()
                .plus(SESSION_DURATION)
                .truncatedTo(ChronoUnit.MILLIS);

        AuthSession session = new AuthSession(
                user,
                tokenHash,
                expiresAt
        );

        authSessionRepository.save(session);

        return new IssuedSession(accessToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(String authorization) {
        String accessToken = extractToken(authorization);
        String tokenHash = hashToken(accessToken);

        AuthSession session = authSessionRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(this::unauthorized);

        if (!session.getExpiresAt().isAfter(Instant.now())) {
            throw unauthorized();
        }

        return MeResponse.from(session.getUser());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256을 사용할 수 없습니다.",
                    e
            );
        }
    }

    private String extractToken(String authorization) {
        if (authorization == null
                || !authorization.regionMatches(
                true, 0, "Bearer ", 0, 7
        )) {
            throw unauthorized();
        }

        String token = authorization.substring(7).strip();

        // 32바이트를 Base64 URL 형식으로 변환한 토큰은 43글자입니다.
        if (!token.matches("[A-Za-z0-9_-]{43}")) {
            throw unauthorized();
        }

        return token;
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "유효한 로그인 세션이 필요합니다."
        );
    }

    public record IssuedSession(
            String accessToken,
            Instant expiresAt
    ) {
    }
}