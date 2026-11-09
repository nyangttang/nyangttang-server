package com.nyangttang.server.device.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;

@Configuration
@Profile("dev")
@EnableScheduling
public class MockSensorConfiguration {
    @Bean
    public Clock studyClock() {
        return Clock.systemUTC();
    }
}
