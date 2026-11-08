package com.nyangttang.server.push;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.concurrent.*;

@Component
public class PushWorker {
    private final PushSettings settings;private final PushJobService jobs;private final PushTransport transport;
    private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"browser-push-worker");t.setDaemon(true);return t;});
    public PushWorker(PushSettings settings,PushJobService jobs,PushTransport transport){this.settings=settings;this.jobs=jobs;this.transport=transport;}
    @PostConstruct public void start(){if(settings.enabled()){
        executor.scheduleWithFixedDelay(this::tick,1,1,TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(()->{try{jobs.cleanup();}catch(Exception e){log("cleanup",e);}},1,24,TimeUnit.HOURS);
    }}
    void tick(){try{
        for(int i=0;i<10;i++){
            var candidate=jobs.claim(Instant.now());if(candidate.isEmpty())break;
            var job=candidate.get();PushTransport.Result result;
            try{result=transport.send(job);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
            catch(Exception e){result=new PushTransport.Result(0,0);log("send",e);}
            jobs.complete(job,result,Instant.now());
            if(result.status()==401 || result.status()==403)LoggerFactory.getLogger(getClass()).warn("Push VAPID 설정 확인 필요: status={}",result.status());
        }
    }catch(Exception e){log("worker",e);}}
    private void log(String action,Exception e){LoggerFactory.getLogger(getClass()).warn("Push {} 실패: {}",action,e.getClass().getSimpleName());}
    @PreDestroy public void stop(){executor.shutdownNow();}
}
