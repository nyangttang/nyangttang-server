package com.nyangttang.server.replay.service;

import com.nyangttang.server.replay.dto.*;
import com.nyangttang.server.replay.repository.RoomEventRepository;
import com.nyangttang.server.study.entity.StudySession;
import com.nyangttang.server.study.repository.StudySessionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class StudyReplayService {
    private static final int SUMMARY_CHUNK_SIZE = 500;
    private final StudySessionRepository sessions;
    private final RoomEventRepository events;

    public StudyReplayService(StudySessionRepository sessions, RoomEventRepository events) {
        this.sessions = sessions; this.events = events;
    }

    public StudyReplayResponse replay(Long userId, Long sessionId, Long afterSeq, int limit) {
        if (limit < 1 || limit > 200) throw badRequest("limit은 1~200 사이여야 합니다.");
        StudySession session = closedSession(userId, sessionId);
        long start = session.getStartEventSeq(), end = session.getEndEventSeq();
        long cursor = afterSeq == null ? start : afterSeq;
        if (cursor < start || cursor > end) {
            throw badRequest("afterSeq는 해당 구간의 startEventSeq~endEventSeq 범위여야 합니다.");
        }
        Long roomId = session.getMember().getRoom().getId();
        var rows = events.findReplayPage(roomId, cursor, end, PageRequest.of(0, limit + 1));
        boolean hasMore = rows.size() > limit;
        var frames = rows.stream().limit(limit).map(StudyReplayResponse.Frame::from).toList();
        Long next = hasMore ? frames.getLast().seq() : null;
        return new StudyReplayResponse(sessionId.toString(), roomId.toString(),
                session.getMember().getId().toString(), session.getStudyDate(),
                session.getStartedAt(), session.getEndedAt(), session.getEndReason(), start, end,
                session.getStartSnapshot(), session.getEndSnapshot(), frames, next, hasMore, summarize(session));
    }

    public StudyReplaySummaryResponse summary(Long userId, Long sessionId) {
        StudySession session = closedSession(userId, sessionId);
        return new StudyReplaySummaryResponse(sessionId.toString(), session.getMember().getRoom().getId().toString(),
                session.getMember().getId().toString(), session.getStudyDate(), session.getStartedAt(),
                session.getEndedAt(), session.getEndReason(), summarize(session));
    }

    private StudySession closedSession(Long userId, Long sessionId) {
        if (sessionId == null || sessionId < 1) throw badRequest("sessionId는 양수여야 합니다.");
        // Ownership is part of the query; a different user's session also returns 404.
        StudySession session = sessions.findByIdAndMember_User_Id(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공부 구간을 찾을 수 없습니다."));
        if (session.getEndedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "진행 중인 공부 구간은 종료 후 조회할 수 있습니다.");
        }
        if (session.getStartSnapshot() == null || session.getEndSnapshot() == null
                || session.getStartEventSeq() == null || session.getEndEventSeq() == null
                || session.getStartEventSeq() < 0 || session.getEndEventSeq() < session.getStartEventSeq()) {
            throw new IllegalStateException("공부 구간의 재생 기록이 불완전합니다.");
        }
        return session;
    }

    private TerritorySummary summarize(StudySession session) {
        String memberId = session.getMember().getId().toString();
        int initial = ownedCount(session.getStartSnapshot(), memberId);
        int last = ownedCount(session.getEndSnapshot(), memberId);
        long gained = 0, lost = 0;
        long cursor = session.getStartEventSeq(), end = session.getEndEventSeq();
        Long roomId = session.getMember().getRoom().getId();
        while (cursor < end) {
            var batch = events.findActionPage(roomId, cursor, end, PageRequest.of(0, SUMMARY_CHUNK_SIZE));
            if (batch.isEmpty()) throw new IllegalStateException("공부 구간의 이벤트 기록이 누락되었습니다.");
            for (var frame : batch) {
                if (frame.seq() != cursor + 1 || frame.seq() > end || frame.actions() == null) {
                    throw new IllegalStateException("공부 구간의 이벤트 순서가 불완전합니다.");
                }
                for (Map<String, Object> action : frame.actions()) {
                    Object type = action.get("type");
                    if (("CLAIM".equals(type) || "INVADE".equals(type)) && memberId.equals(action.get("memberId"))) {
                        gained = Math.addExact(gained, 1L);
                    }
                    if ("INVADE".equals(type) && memberId.equals(action.get("targetMemberId"))) {
                        lost = Math.addExact(lost, 1L);
                    }
                }
                cursor = frame.seq();
            }
        }
        if (gained - lost != last - initial) {
            throw new IllegalStateException("점령 이벤트와 시작·종료 지도가 일치하지 않습니다.");
        }
        return new TerritorySummary(session.getCreditedMs(), gained, lost, initial, last, last - initial);
    }

    private static int ownedCount(Map<String, Object> snapshot, String memberId) {
        Object raw = snapshot.get("owners");
        if (!(raw instanceof List<?> owners) || owners.size() != 100) {
            throw new IllegalStateException("저장된 재생 지도는 100칸이어야 합니다.");
        }
        return (int) owners.stream().filter(memberId::equals).count();
    }
    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
