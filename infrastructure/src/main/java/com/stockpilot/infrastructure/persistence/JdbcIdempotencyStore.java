package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.idempotency.IdempotencyRecord;
import com.stockpilot.core.port.out.IdempotencyStore;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcIdempotencyStore implements IdempotencyStore {

    private final NamedParameterJdbcTemplate jdbc;

    JdbcIdempotencyStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean reserve(String scope, String key, String requestHash) {
        int inserted = jdbc.update("""
                INSERT INTO idempotency_key (scope, key, request_hash, status)
                VALUES (:scope, :key, :hash, 'IN_PROGRESS')
                ON CONFLICT (scope, key) DO NOTHING
                """, params(scope, key).addValue("hash", requestHash));
        return inserted == 1;
    }

    @Override
    public Optional<IdempotencyRecord> find(String scope, String key) {
        return jdbc.query("""
                SELECT scope, key, request_hash, status, response_snapshot, created_at
                FROM idempotency_key WHERE scope = :scope AND key = :key
                """, params(scope, key), (rs, i) -> new IdempotencyRecord(rs.getString("scope"), rs.getString("key"),
                rs.getString("request_hash"), IdempotencyRecord.Status.valueOf(rs.getString("status")),
                rs.getString("response_snapshot"), rs.getTimestamp("created_at").toInstant())).stream().findFirst();
    }

    private MapSqlParameterSource params(String scope, String key) {
        return new MapSqlParameterSource("scope", scope).addValue("key", key);
    }

    @Override
    public void complete(String scope, String key, String responseSnapshot) {
        jdbc.update("""
                UPDATE idempotency_key SET status = 'COMPLETED', response_snapshot = :snapshot, completed_at = now()
                WHERE scope = :scope AND key = :key
                """, params(scope, key).addValue("snapshot", responseSnapshot));
    }
}
