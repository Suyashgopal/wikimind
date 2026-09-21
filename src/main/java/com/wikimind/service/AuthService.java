package com.wikimind.service;

import com.wikimind.dto.AuthResponse;
import com.wikimind.dto.LoginRequest;
import com.wikimind.dto.RegisterRequest;
import com.wikimind.exception.UsernameTakenException;
import com.wikimind.model.AppUser;
import com.wikimind.model.Role;
import com.wikimind.repository.AppUserRepository;
import com.wikimind.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Self-registration always creates a MEMBER; roles are never taken from the request. */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameTakenException(request.username());
        }
        AppUser user = userRepository.save(new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                Role.MEMBER,
                request.organization().strip().toLowerCase()));
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("invalid username or password");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(AppUser user) {
        return new AuthResponse(jwtService.generateToken(user), user.getRole().name(), user.getOrgId());
    }
}
