package com.example.jobanalyzer.service;

import com.example.jobanalyzer.entity.User;
import com.example.jobanalyzer.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.jobanalyzer.dto.LoginRequest;
import com.example.jobanalyzer.dto.LoginResponse;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse register(LoginRequest loginRequest) {
        if(userRepository.findByUsername(loginRequest.getUsername()).isPresent()) {
            throw new BadCredentialsException("Username already taken");
        }

        User user = new User(loginRequest.getUsername(),
                passwordEncoder.encode(loginRequest.getPassword()));

        userRepository.save(user);
        String token = jwtService.generateToken(loginRequest.getUsername());
        return new LoginResponse(token);
    }



    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if(!passwordEncoder.matches(loginRequest.getPassword(),user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(loginRequest.getUsername());
        return new LoginResponse(token);
    }
}
