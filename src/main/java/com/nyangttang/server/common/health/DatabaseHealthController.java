package com.nyangttang.server.common.health;

import com.nyangttang.server.common.error.ApiException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class DatabaseHealthController {
    private final JdbcTemplate jdbcTemplate;
    public DatabaseHealthController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate=jdbcTemplate; }
    @GetMapping("/api/v1/health/db")
    public ResponseEntity<?> checkDatabase() {
        final String database;
        try { database=jdbcTemplate.queryForObject("SELECT DATABASE()",String.class); }
        catch(DataAccessException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"DATABASE_UNAVAILABLE","데이터베이스 연결을 확인하세요.");
        }
        if(!"nyangttang".equals(database))
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"DATABASE_MISMATCH","연결된 데이터베이스를 확인하세요.");
        return ResponseEntity.ok(Map.of("data",Map.of("status","ok","database",database)));
    }
}
