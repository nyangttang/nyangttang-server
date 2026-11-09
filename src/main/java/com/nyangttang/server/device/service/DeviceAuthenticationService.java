package com.nyangttang.server.device.service;

import com.nyangttang.server.common.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Service
@Profile({"dev", "prod"})
public class DeviceAuthenticationService {
    public static final String HEADER = "X-Device-Key";
    private final Map<String, byte[]> keyHashes;
    private static final byte[] UNKNOWN_KEY_HASH = sha256("unknown-device");

    public DeviceAuthenticationService(@Value("${DEVICE_KEYS:}") String configured,
                                       Environment environment) {
        Map<String, byte[]> hashes = new HashMap<>();
        if (configured != null && !configured.isBlank()) {
            for (String entry : configured.split(",", -1)) {
                String[] pair = entry.strip().split("=", -1);
                if (pair.length != 2) throw invalidConfiguration();
                String code = pair[0].strip();
                String key = pair[1].strip();
                if (code.length() < 3 || code.length() > 32
                        || !code.matches("[A-Z0-9]+(?:-[A-Z0-9]+)*")
                        || code.startsWith("SIM-") || !key.matches("[a-f0-9]{64}")
                        || hashes.putIfAbsent(code, sha256(key)) != null) {
                    throw invalidConfiguration();
                }
            }
        }
        if (environment.acceptsProfiles(Profiles.of("prod")) && hashes.isEmpty()) {
            throw new IllegalStateException("prod 실행에는 DEVICE_KEYS 설정이 필요합니다.");
        }
        keyHashes = Map.copyOf(hashes);
    }

    public void requireValidKey(String deviceCode, String suppliedKey) {
        byte[] expected = deviceCode == null ? null : keyHashes.get(deviceCode);
        boolean validShape = suppliedKey != null && suppliedKey.matches("[a-f0-9]{64}");
        byte[] actual = sha256(validShape ? suppliedKey : "invalid-device-key");
        boolean same = MessageDigest.isEqual(expected == null ? UNKNOWN_KEY_HASH : expected, actual);
        if (!validShape || expected == null || !same) {
            // Unknown code, absent key and wrong key have the same external response.
            throw new ApiException(HttpStatus.UNAUTHORIZED, "DEVICE_AUTH_FAILED",
                    "기기 코드와 X-Device-Key를 확인해주세요.");
        }
    }

    private static IllegalArgumentException invalidConfiguration() {
        // Never include configured keys in exceptions or logs.
        return new IllegalArgumentException(
                "DEVICE_KEYS 형식 오류: 기기코드=64자리 소문자 hex 키를 쉼표로 구분하고 중복 코드를 제거해주세요.");
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", impossible);
        }
    }
}
