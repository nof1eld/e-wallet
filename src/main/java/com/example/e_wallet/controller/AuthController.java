package com.example.e_wallet.controller;

import com.example.e_wallet.dto.LoginRequest;
import com.example.e_wallet.dto.LoginResponse;
import com.example.e_wallet.dto.RegisterRequest;
import com.example.e_wallet.entity.User;
import com.example.e_wallet.repository.UserRepository;
import com.example.e_wallet.service.JwtService;
import com.example.e_wallet.exception.InvalidCredentialsException;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));


        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token);
    }
    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new InvalidCredentialsException("Username already taken");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(User.Role.USER);   // an admin user will need to be added manually
        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token);
    }
}