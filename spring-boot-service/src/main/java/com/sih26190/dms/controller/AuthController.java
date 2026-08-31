package com.sih26190.dms.controller;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sih26190.dms.dto.LoginRequest;
import com.sih26190.dms.dto.LoginResponse;
import com.sih26190.dms.dto.RegisterRequest;
import com.sih26190.dms.model.User;
import com.sih26190.dms.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        userRepository.save(user);

        return ResponseEntity.ok("User registered");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> {
                    String credentials = request.getUsername() + ":" + request.getPassword();
                    String token = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                    return ResponseEntity.ok(new LoginResponse(token, user.getRole().name(), user.getUsername()));
                })
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

}
