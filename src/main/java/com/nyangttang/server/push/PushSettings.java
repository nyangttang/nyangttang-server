package com.nyangttang.server.push;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record PushSettings(boolean enabled, String publicKey, String privateKey, String subject) {
    public PushSettings(@Value("${PUSH_ENABLED:false}") boolean enabled,
                        @Value("${VAPID_PUBLIC_KEY:}") String publicKey,
                        @Value("${VAPID_PRIVATE_KEY:}") String privateKey,
                        @Value("${VAPID_SUBJECT:}") String subject) {
        this.enabled=enabled; this.publicKey=publicKey; this.privateKey=privateKey; this.subject=subject;
    }
    @Override public String toString() { return "PushSettings[enabled="+enabled+"]"; }
}
