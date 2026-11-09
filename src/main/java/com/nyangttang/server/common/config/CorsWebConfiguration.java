package com.nyangttang.server.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.net.URI;
import java.util.Arrays;

@Configuration
public class CorsWebConfiguration implements WebMvcConfigurer {
    private final String[] origins;

    public CorsWebConfiguration(@Value("${FRONTEND_ORIGINS:http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000}") String configuredOrigins) {
        origins = parseOrigins(configuredOrigins);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "X-Device-Key")
                .exposedHeaders("X-Request-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }

    static String[] parseOrigins(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException("FRONTEND_ORIGINS에 프론트 주소를 입력해주세요.");
        }
        return Arrays.stream(configured.split(",", -1)).map(value -> {
            String origin = value.strip().replaceAll("/+$", "");
            URI uri;
            try { uri = URI.create(origin); }
            catch (IllegalArgumentException invalid) { throw invalidOrigin(); }
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                    || uri.getPort() < -1 || uri.getPort() == 0 || uri.getPort() > 65535) {
                throw invalidOrigin();
            }
            return origin;
        }).distinct().toArray(String[]::new);
    }

    private static IllegalArgumentException invalidOrigin() {
        return new IllegalArgumentException("FRONTEND_ORIGINS에는 경로 없이 http(s)://호스트:포트 형식의 주소를 쉼표로 구분해 입력해주세요. 와일드카드는 사용할 수 없습니다.");
    }
}
