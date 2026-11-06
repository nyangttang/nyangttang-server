package com.nyangttang.server.common.health;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DatabaseHealthController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/v1/health/db")
    public ResponseEntity<?> checkDatabase() {
        try {
            String database = jdbcTemplate.queryForObject(
                    "SELECT DATABASE()",
                    String.class
            );

            if (!"nyangttang".equals(database)) {
                return ResponseEntity.status(503).body(
                        Map.of("error", Map.of(
                                "code", "DATABASE_MISMATCH",
                                "message", "연결된 데이터베이스를 확인하세요."
                        ))
                );
            }

            return ResponseEntity.ok(
                    Map.of("data", Map.of(
                            "status", "ok",
                            "database", database
                    ))
            );
        } catch (DataAccessException exception) {
            return ResponseEntity.status(503).body(
                    Map.of("error", Map.of(
                            "code", "DATABASE_UNAVAILABLE",
                            "message", "데이터베이스 연결을 확인하세요."
                    ))
            );
        }
    }
}