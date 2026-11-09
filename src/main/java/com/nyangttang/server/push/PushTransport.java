package com.nyangttang.server.push;

public interface PushTransport {
    record Result(int status,long retryAfterSeconds) {}
    Result send(PushJobService.Job job) throws Exception;
}
