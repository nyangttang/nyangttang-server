package com.nyangttang.server.push;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PushValidation {
    static { if (Security.getProvider("BC")==null) Security.addProvider(new BouncyCastleProvider()); }
    private PushValidation() {}
    public static URI endpoint(String endpoint) {
        try {
            if(endpoint==null || endpoint.length()>2048) throw new IllegalArgumentException();
            URI u=URI.create(endpoint);String host=u.getHost();
            boolean allowed=host!=null && (Set.of("fcm.googleapis.com","updates.push.services.mozilla.com",
                    "web.push.apple.com").contains(host) || host.endsWith(".notify.windows.com"));
            if(!"https".equals(u.getScheme()) || !allowed || (u.getPort()!=-1 && u.getPort()!=443)
                    || u.getRawUserInfo()!=null || u.getRawFragment()!=null || u.getRawPath().isEmpty())
                throw new IllegalArgumentException();
            return u;
        } catch(RuntimeException e) { throw bad("지원되는 브라우저의 HTTPS Push endpoint가 필요합니다."); }
    }
    public static void keys(String p256dh,String auth) {
        try {
            byte[] key=Base64.getUrlDecoder().decode(p256dh), secret=Base64.getUrlDecoder().decode(auth);
            if(key.length!=65 || key[0]!=4 || secret.length!=16) throw new IllegalArgumentException();
            Utils.loadPublicKey(p256dh); // Validates the P-256 point, not just its string length.
        } catch(Exception e) { throw bad("브라우저에서 생성한 p256dh/auth 키가 필요합니다."); }
    }
    public static String hash(String endpoint) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(endpoint.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
    private static ResponseStatusException bad(String reason){return new ResponseStatusException(HttpStatus.BAD_REQUEST,reason);}
}
