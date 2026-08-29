package com.roshan.resourcebooking.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.roshan.resourcebooking.entity.Role;
import com.roshan.resourcebooking.entity.User;
import com.roshan.resourcebooking.repository.UserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        createOrUpdateUser(
                "admin",
                "admin@123",
                Role.ADMIN
        );

        createOrUpdateUser(
                "user",
                "user@123",
                Role.USER
        );
    }

    private void createOrUpdateUser(
            String username,
            String rawPassword,
            Role role) {

        User user = userRepository
                .findByUsername(username)
                .orElseGet(User::new);

        user.setUsername(username);

        // Password is automatically converted to BCrypt.
        // You only need to remember the plain password.
        user.setPassword(passwordEncoder.encode(rawPassword));

        user.setRole(role);

        userRepository.save(user);
    }
}