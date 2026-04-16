package com.bank.management.controller;

import com.bank.management.dto.JwtResponse;
import com.bank.management.dto.LoginRequest;
import com.bank.management.dto.SignupRequest;
import com.bank.management.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    // Optional: user self-registration (if allowed)
    // For this system, users are created by managers; this endpoint can be disabled or used for demo.
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        authService.registerUser(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully! Waiting for manager approval.");
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        // Since JWT is stateless, client just discards the token.
        return ResponseEntity.ok("Logged out successfully!");
    }
}