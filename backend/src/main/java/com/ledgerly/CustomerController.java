package com.ledgerly;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final JdbcTemplate database;

    public CustomerController(JdbcTemplate database) {
        this.database = database;
    }

    @GetMapping
    public List<Map<String, Object>> getCustomers() {
        return database.queryForList("SELECT * FROM customers ORDER BY id");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCustomer(@PathVariable long id) {
        List<Map<String, Object>> customers = database.queryForList(
                "SELECT * FROM customers WHERE id = ?", id);
        if (customers.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customers.get(0));
    }

    @PostMapping
    public Map<String, String> addCustomer(@RequestBody Map<String, String> customer) {
        requireCustomerFields(customer);
        database.update(
                "INSERT INTO customers (name, email, phone) VALUES (?, ?, ?)",
                customer.get("name"),
                customer.get("email"),
                customer.get("phone"));
        return customer;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateCustomer(
            @PathVariable long id,
            @RequestBody Map<String, String> customer) {
        requireCustomerFields(customer);
        int changed = database.update(
                "UPDATE customers SET name = ?, email = ?, phone = ? WHERE id = ?",
                customer.get("name"),
                customer.get("email"),
                customer.get("phone"),
                id);
        if (changed == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable long id) {
        int changed = database.update("DELETE FROM customers WHERE id = ?", id);
        if (changed == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private void requireCustomerFields(Map<String, String> customer) {
        if (customer == null
                || isBlank(customer.get("name"))
                || isBlank(customer.get("email"))
                || isBlank(customer.get("phone"))) {
            throw new IllegalArgumentException("name, email, and phone are required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
