package com.nyangttang.server.game.service;

import com.nyangttang.server.study.service.StudyTimeline;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;

@Component
public class MovementTimeline {
    // Input slices already end at midnight/grace/heartbeat boundaries.
    // Split again at every earned step so each move gets its own replay frame.
    public List<StudyTimeline.Slice> split(List<StudyTimeline.Slice> source, int remainder, int stepMillis) {
        if (stepMillis <= 0 || stepMillis > 60_000 || remainder < 0 || remainder >= stepMillis) {
            throw new IllegalArgumentException("이동 시간 설정이 잘못되었습니다.");
        }
        List<StudyTimeline.Slice> result = new ArrayList<>();
        int carry = remainder;
        for (var slice : source) {
            long credit = slice.creditedMs();
            if (credit == 0) { result.add(slice); continue; }
            if (credit < 0 || credit > java.time.Duration.between(slice.from(), slice.to()).toMillis()) {
                throw new IllegalArgumentException("인정 시간이 구간 범위를 벗어났습니다.");
            }
            Instant cursor = slice.from();
            while (credit > 0) {
                long chunk = Math.min(credit, stepMillis - carry);
                Instant end = cursor.plusMillis(chunk);
                result.add(new StudyTimeline.Slice(slice.date(), cursor, end, chunk));
                carry = (int) ((carry + chunk) % stepMillis);
                credit -= chunk;
                cursor = end;
            }
            if (cursor.isBefore(slice.to())) {
                result.add(new StudyTimeline.Slice(slice.date(), cursor, slice.to(), 0));
            }
        }
        return result;
    }
}
