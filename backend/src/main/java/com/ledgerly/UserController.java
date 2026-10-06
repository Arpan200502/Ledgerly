package com.ledgerly;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final JdbcTemplate database;
    private static final String SALT = "ledgerly-course-project";

    public UserController(JdbcTemplate database) {
        this.database = database;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Long id = findUserIdByToken(auth);
        if (id == null) {
            return unauthorized();
        }
        Map<String, Object> user = database.queryForMap(
                "SELECT id, name, email, phone, created_at FROM users WHERE id = ?", id);
        return ResponseEntity.ok(sanitize(user));
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMe(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, String> body) {
        Long id = findUserIdByToken(auth);
        if (id == null) {
            return unauthorized();
        }

        String name = body.get("name");
        String email = body.get("email");
        String phone = body.get("phone");

        if (name != null && !name.isBlank()) {
            database.update("UPDATE users SET name = ? WHERE id = ?", name.trim(), id);
        }
        if (email != null && !email.isBlank()) {
            String normalizedEmail = email.trim().toLowerCase();
            List<Map<String, Object>> clash = database.queryForList(
                    "SELECT id FROM users WHERE email = ? AND id <> ?", normalizedEmail, id);
            if (!clash.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "email already in use"));
            }
            database.update("UPDATE users SET email = ? WHERE id = ?", normalizedEmail, id);
        }
        if (phone != null) {
            database.update("UPDATE users SET phone = ? WHERE id = ?", phone, id);
        }

        Map<String, Object> user = database.queryForMap(
                "SELECT id, name, email, phone, created_at FROM users WHERE id = ?", id);
        return ResponseEntity.ok(sanitize(user));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, String> body) {
        Long id = findUserIdByToken(auth);
        if (id == null) {
            return unauthorized();
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (isBlank(oldPassword) || isBlank(newPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "oldPassword and newPassword are required"));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "new password must be at least 6 characters"));
        }

        Map<String, Object> row = database.queryForMap(
                "SELECT password_hash FROM users WHERE id = ?", id);
        if (!hash(oldPassword).equals(row.get("password_hash"))) {
            return ResponseEntity.status(401).body(Map.of("error", "old password is incorrect"));
        }

        database.update("UPDATE users SET password_hash = ? WHERE id = ?", hash(newPassword), id);
        return ResponseEntity.ok(Map.of("message", "password updated successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String token = extractToken(auth);
        if (token != null) {
            database.update("UPDATE users SET token = NULL WHERE token = ?", token);
        }
        return ResponseEntity.ok(Map.of("message", "logged out"));
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "unauthorized — please login again"));
    }

    private Long findUserIdByToken(String auth) {
        String token = extractToken(auth);
        if (token == null) {
            return null;
        }
        List<Map<String, Object>> rows = database.queryForList(
                "SELECT id FROM users WHERE token = ?", token);
        if (rows.isEmpty()) {
            return null;
        }
        return ((Number) rows.get(0).get("id")).longValue();
    }

    private String extractToken(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        String token = auth.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private Map<String, Object> sanitize(Map<String, Object> user) {
        return Map.of(
                "id", user.get("id"),
                "name", user.get("name"),
                "email", user.get("email"),
                "phone", user.get("phone") == null ? "" : user.get("phone"),
                "created_at", String.valueOf(user.get("created_at")));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((SALT + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
