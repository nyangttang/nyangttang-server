package com.nyangttang.server.study.service;

import com.nyangttang.server.room.repository.RoomMemberRepository;
import com.nyangttang.server.study.dto.*;
import com.nyangttang.server.study.entity.DailyStudyStat;
import com.nyangttang.server.study.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class StudyQueryService {
    private final RoomMemberRepository members;
    private final DailyStudyStatRepository daily;
    private final StudySessionRepository sessions;

    public StudyQueryService(RoomMemberRepository members, DailyStudyStatRepository daily,
                             StudySessionRepository sessions) {
        this.members = members; this.daily = daily; this.sessions = sessions;
    }

    public DailyStudyResponse daily(Long userId, LocalDate from, LocalDate to) {
        validateDate(from); validateDate(to);
        long dayCount = ChronoUnit.DAYS.between(from, to) + 1;
        if (dayCount < 1 || dayCount > 366) {
            throw badRequest("조회 기간은 시작일과 종료일을 포함하여 1~366일이어야 합니다.");
        }
        Map<LocalDate,DailyStudyStat> byDate=new HashMap<>();
        Map<LocalDate,Long> totals=new HashMap<>();
        for(var stat:daily.findAllByMember_User_IdAndStudyDateBetweenOrderByStudyDateAscIdAsc(userId,from,to)) {
            byDate.put(stat.getStudyDate(),stat); // Most recently created membership/day supplies the displayed goal.
            totals.merge(stat.getStudyDate(),stat.getCreditedMs(),Math::addExact);
        }
        var result = new ArrayList<DailyStudyResponse.Day>((int) dayCount);
        long total = 0;
        for (int i = 0; i < dayCount; i++) {
            LocalDate date = from.plusDays(i);
            var stat = byDate.get(date);
            long millis = totals.getOrDefault(date,0L);
            total = Math.addExact(total, millis);
            result.add(new DailyStudyResponse.Day(date, millis, millis / 1000,
                    stat == null ? null : stat.getGoalSeconds(),
                    stat != null && millis >= stat.getGoalSeconds()*1000L));
        }
        return new DailyStudyResponse(from, to, "Asia/Seoul", total, List.copyOf(result));
    }

    public StudySessionListResponse sessions(Long userId, LocalDate date, Long beforeId, int limit) {
        validateDate(date);
        if (limit < 1 || limit > 50) throw badRequest("limit은 1~50 사이여야 합니다.");
        if (beforeId != null && beforeId < 1) throw badRequest("beforeId는 양수여야 합니다.");
        // Fetch one extra row to determine hasMore, without a count query.
        var page = PageRequest.of(0, limit + 1);
        List<StudySessionSummary> rows = beforeId == null
                ? sessions.findSummaryPage(userId, date, page)
                : sessions.findSummaryPageBefore(userId, date, beforeId, page);
        boolean hasMore = rows.size() > limit;
        var items = rows.stream().limit(limit).map(StudySessionListResponse.Item::from).toList();
        String next = hasMore ? items.getLast().sessionId() : null;
        return new StudySessionListResponse(items, next, hasMore);
    }

    private static void validateDate(LocalDate date) {
        if (date == null || date.getYear() < 1000 || date.getYear() > 9999) {
            throw badRequest("날짜는 1000-01-01~9999-12-31 범위의 yyyy-MM-dd 형식이어야 합니다.");
        }
    }
    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
