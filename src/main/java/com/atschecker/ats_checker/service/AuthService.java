package com.atschecker.ats_checker.service;

import com.atschecker.ats_checker.entity.User;
import com.atschecker.ats_checker.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(10));
        String role = "ROLE_USER";

        if (userRepository.count() == 0) {
            role = "ROLE_ADMIN";
        }

        User user = new User(username, hashedPassword, email, role);
        return userRepository.save(user);
    }

    public Optional<User> login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && BCrypt.checkpw(password, userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }
}
