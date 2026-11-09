package com.nyangttang.server.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@Profile("prod")
public class ProductionStartupGuard {
    public ProductionStartupGuard(Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            throw new IllegalStateException("prod와 dev를 동시에 활성화할 수 없습니다.");
        }
        String origins = environment.getProperty("FRONTEND_ORIGINS");
        if (origins == null || origins.isBlank()) {
            throw new IllegalStateException("prod 실행에는 FRONTEND_ORIGINS 설정이 필요합니다.");
        }
        CorsWebConfiguration.parseOrigins(origins);
    }
}
