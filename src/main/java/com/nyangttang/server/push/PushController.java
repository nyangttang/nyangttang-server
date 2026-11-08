package com.nyangttang.server.push;

import com.nyangttang.server.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/push")
public class PushController {
    private final AuthService auth;private final PushSubscriptionService service;private final PushSettings settings;
    public PushController(AuthService auth,PushSubscriptionService service,PushSettings settings){this.auth=auth;this.service=service;this.settings=settings;}
    @GetMapping("/config") public ResponseEntity<?> config(){return ok(Map.of("enabled",settings.enabled(),"publicKey",settings.enabled()?settings.publicKey():""));}
    @GetMapping("/subscriptions") public ResponseEntity<?> list(@RequestHeader(value="Authorization",required=false) String h){return ok(service.list(auth.getMe(h).id()));}
    @PostMapping("/subscriptions") public ResponseEntity<?> subscribe(@RequestHeader(value="Authorization",required=false) String h,@Valid @RequestBody PushInput input){return ok(service.subscribe(auth.getMe(h).id(),input));}
    @PostMapping("/subscriptions/{id}/unsubscribe") public ResponseEntity<?> unsubscribe(@RequestHeader(value="Authorization",required=false) String h,@PathVariable("id") Long id){return ok(service.unsubscribe(auth.getMe(h).id(),id));}
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","구독 등록이 충돌했습니다. 다시 확인해주세요."));}
    private ResponseEntity<?> ok(Object data){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("data",data));}
}
