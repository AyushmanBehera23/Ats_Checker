package com.atschecker.ats_checker.controller;

import com.atschecker.ats_checker.entity.User;
import com.atschecker.ats_checker.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");

        if (username == null || password == null || email == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields are required"));
        }
        try {
            User user = authService.register(username, password, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User registered successfully",
                    "username", user.getUsername(),
                    "role", user.getRole()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required"));
        }

        Optional<User> userOpt = authService.login(username, password);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }

        User user = userOpt.get();
        session.setAttribute("user", user);
        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "username", user.getUsername(),
                "role", user.getRole()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }
        return ResponseEntity.ok(Map.of(
                "loggedIn", true,
                "username", user.getUsername(),
                "role", user.getRole()));
    }
}
