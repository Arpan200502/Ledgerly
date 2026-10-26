package com.ledgerly;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JdbcTemplate database;
    private static final String SALT = "ledgerly-course-project";

    public AuthController(JdbcTemplate database) {
        this.database = database;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String phone = body.get("phone");

        if (isBlank(name) || isBlank(email) || isBlank(password)) {
            return badRequest("name, email and password are required");
        }
        if (password.length() < 6) {
            return badRequest("password must be at least 6 characters");
        }

        String normalizedEmail = email.trim().toLowerCase();
        List<Map<String, Object>> existing = database.queryForList(
                "SELECT id FROM users WHERE email = ?", normalizedEmail);
        if (!existing.isEmpty()) {
            return badRequest("an account with this email already exists");
        }

        String token = UUID.randomUUID().toString();
        database.update(
                "INSERT INTO users (name, email, phone, password_hash, token) VALUES (?, ?, ?, ?, ?)",
                name.trim(), normalizedEmail, phone, hash(password), token);

        Map<String, Object> user = fetchSafeUserByEmail(normalizedEmail);
        return ResponseEntity.ok(Map.of("token", token, "user", user));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (isBlank(email) || isBlank(password)) {
            return badRequest("email and password are required");
        }

        String normalizedEmail = email.trim().toLowerCase();
        List<Map<String, Object>> users = database.queryForList(
                "SELECT * FROM users WHERE email = ?", normalizedEmail);
        if (users.isEmpty()) {
            return unauthorized();
        }

        Map<String, Object> user = users.get(0);
        String storedHash = (String) user.get("password_hash");
        if (!hash(password).equals(storedHash)) {
            return unauthorized();
        }

        String token = UUID.randomUUID().toString();
        database.update("UPDATE users SET token = ? WHERE id = ?", token, user.get("id"));

        Map<String, Object> safeUser = Map.of(
                "id", user.get("id"),
                "name", user.get("name"),
                "email", user.get("email"),
                "phone", user.get("phone") == null ? "" : user.get("phone"),
                "created_at", String.valueOf(user.get("created_at")));

        return ResponseEntity.ok(Map.of("token", token, "user", safeUser));
    }

    private Map<String, Object> fetchSafeUserByEmail(String email) {
        Map<String, Object> user = database.queryForMap(
                "SELECT id, name, email, phone, created_at FROM users WHERE email = ?", email);
        return Map.of(
                "id", user.get("id"),
                "name", user.get("name"),
                "email", user.get("email"),
                "phone", user.get("phone") == null ? "" : user.get("phone"),
                "created_at", String.valueOf(user.get("created_at")));
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "invalid email or password"));
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
