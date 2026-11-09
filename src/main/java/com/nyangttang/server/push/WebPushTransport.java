package com.nyangttang.server.push;

import nl.martijndwars.webpush.*;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest;
import java.time.*;
import java.time.format.DateTimeFormatter;

@Component
public class WebPushTransport implements PushTransport {
    // Use the library only for RFC 8291 encryption and VAPID signing.
    static class Encoder extends AbstractPushService<Encoder> {
        Encoder(String pub,String priv,String subject)throws Exception{super(pub,priv,subject);}
        nl.martijndwars.webpush.HttpRequest encode(Notification n)throws Exception{return prepareRequest(n,Encoding.AES128GCM);}
    }
    private final Encoder encoder;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    public WebPushTransport(PushSettings settings)throws Exception {
        if(!settings.enabled()){encoder=null;return;}
        PushValidation.keys(settings.publicKey(),"AAAAAAAAAAAAAAAAAAAAAA");
        String subject=settings.subject();
        if(!(subject.startsWith("mailto:") && subject.substring(7).contains("@")) &&
                !(subject.startsWith("https://") && URI.create(subject).getHost()!=null))
            throw new IllegalStateException("VAPID_SUBJECT에 연락 가능한 mailto: 또는 HTTPS 주소가 필요합니다.");
        encoder=new Encoder(settings.publicKey(),settings.privateKey(),subject);
        if(!Utils.verifyKeyPair(encoder.getPrivateKey(),encoder.getPublicKey()))throw new IllegalStateException("VAPID 키 쌍이 일치하지 않습니다.");
    }
    public Result send(PushJobService.Job job)throws Exception {
        if(encoder==null)throw new IllegalStateException("Push 비활성 상태");
        var request=prepare(job);
        var response=client.send(request,HttpResponse.BodyHandlers.discarding());
        return new Result(response.statusCode(),retryAfter(response.headers().firstValue("Retry-After").orElse(""),Instant.now()));
    }
    HttpRequest prepare(PushJobService.Job job)throws Exception {
        PushValidation.endpoint(job.endpoint());
        var prepared=encoder.encode(new Notification(job.endpoint(),job.p256dh(),job.auth(),job.payload(),job.ttl()));
        var builder= HttpRequest.newBuilder(PushValidation.endpoint(prepared.getUrl())).timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofByteArray(prepared.getBody()));
        prepared.getHeaders().forEach(builder::header);return builder.build();
    }
    static long retryAfter(String value,Instant now){
        try{return Math.max(0,Long.parseLong(value));}catch(NumberFormatException ignored){}
        try{return Math.max(0,Duration.between(now,ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds());}
        catch(RuntimeException ignored){return 0;}
    }
    @PreDestroy public void close(){client.close();}
}
