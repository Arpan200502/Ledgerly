package com.ledgerly;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Query history for the chat sidebar.
 * Every row belongs to one user (taken from the Bearer token), so users only
 * ever see their own history.
 */
@RestController
@RequestMapping("/history")
public class HistoryController {

    private static final Set<String> TYPES = Set.of("READ", "WRITE", "CHAT");
    private static final Set<String> STATUSES = Set.of("SUCCESS", "PENDING", "CONFIRMED", "FAILED");

    private final JdbcTemplate db;

    public HistoryController(JdbcTemplate db) {
        this.db = db;
    }

    /**
     * GET /history?q=sharma -> latest 50 entries, optional search on the question
     * text.
     */
    @GetMapping
    public ResponseEntity<Object> list(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(value = "q", required = false) String q) {
        Long userId = userIdFromToken(auth);
        if (userId == null) {
            return unauthorized();
        }
        String like = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        List<Map<String, Object>> rows = db.queryForList(
                "SELECT id, question, generated_sql, query_type, status, reply, created_at "
                        + "FROM query_history WHERE user_id = ? AND LOWER(question) LIKE ? "
                        + "ORDER BY created_at DESC, id DESC LIMIT 50",
                userId, like);
        return ResponseEntity.ok(rows);
    }

    /** POST /history -> saves one entry, returns {"id": ...}. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> save(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody HistoryEntry entry) {
        Long userId = userIdFromToken(auth);
        if (userId == null) {
            return unauthorized();
        }
        if (entry == null || entry.question() == null || entry.question().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "question is required"));
        }
        String type = TYPES.contains(entry.queryType()) ? entry.queryType() : "CHAT";
        String status = STATUSES.contains(entry.status()) ? entry.status() : "SUCCESS";
        Long id = db.queryForObject(
                "INSERT INTO query_history (user_id, question, generated_sql, query_type, status, reply) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                userId,
                cut(entry.question(), 4000),
                cut(entry.generatedSql(), 8000),
                type,
                status,
                cut(entry.reply(), 2000));
        return ResponseEntity.ok(Map.of("id", id));
    }

    /** PATCH /history/{id} -> moves a PENDING write to CONFIRMED or FAILED. */
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updateStatus(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable("id") long id,
            @RequestBody StatusUpdate update) {
        Long userId = userIdFromToken(auth);
        if (userId == null) {
            return unauthorized();
        }
        if (update == null || !STATUSES.contains(update.status())) {
            return ResponseEntity.badRequest().body(Map.of("error", "valid status is required"));
        }
        int changed = db.update(
                "UPDATE query_history SET status = ?, reply = COALESCE(?, reply) WHERE id = ? AND user_id = ?",
                update.status(), cut(update.reply(), 2000), id, userId);
        if (changed == 0) {
            return ResponseEntity.status(404).body(Map.of("error", "history entry not found"));
        }
        return ResponseEntity.ok(Map.of("updated", true));
    }

    /** DELETE /history -> clears the current user's history. */
    @DeleteMapping
    public ResponseEntity<Object> clear(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Long userId = userIdFromToken(auth);
        if (userId == null) {
            return unauthorized();
        }
        int removed = db.update("DELETE FROM query_history WHERE user_id = ?", userId);
        return ResponseEntity.ok(Map.of("deleted", removed));
    }

    // ---- helpers ----

    private Long userIdFromToken(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        String token = auth.substring(7).trim();
        if (token.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> rows = db.queryForList("SELECT id FROM users WHERE token = ?", token);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("id")).longValue();
    }

    private ResponseEntity<Object> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "unauthorized — please login again"));
    }

    private static String cut(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record HistoryEntry(String question, String generatedSql, String queryType, String status, String reply) {
    }

    public record StatusUpdate(String status, String reply) {
    }
}