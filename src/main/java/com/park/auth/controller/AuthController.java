package com.park.auth.controller;

import com.park.auth.entity.User;
import com.park.auth.service.AuthService;
import com.park.common.dto.AuthRequest;
import com.park.common.dto.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody AuthRequest request) {
        // Default to USER role for registration endpoint
        authService.register(request.getUsername(), request.getPassword(), User.UserRole.USER);
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }
    
    @PostMapping("/register/admin")
    public ResponseEntity<Map<String, String>> registerAdmin(@Valid @RequestBody AuthRequest request) {
        authService.register(request.getUsername(), request.getPassword(), User.UserRole.ADMIN);
        return ResponseEntity.ok(Map.of("message", "Admin registered successfully"));
    }
    
    @PostMapping("/register/attendant")
    public ResponseEntity<Map<String, String>> registerAttendant(@Valid @RequestBody AuthRequest request) {
        authService.register(request.getUsername(), request.getPassword(), User.UserRole.ATTENDANT);
        return ResponseEntity.ok(Map.of("message", "Attendant registered successfully"));
    }
}
