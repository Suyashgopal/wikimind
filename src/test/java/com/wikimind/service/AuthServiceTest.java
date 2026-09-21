package com.wikimind.service;

import com.wikimind.dto.AuthResponse;
import com.wikimind.dto.LoginRequest;
import com.wikimind.dto.RegisterRequest;
import com.wikimind.exception.UsernameTakenException;
import com.wikimind.model.AppUser;
import com.wikimind.model.Role;
import com.wikimind.repository.AppUserRepository;
import com.wikimind.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerAlwaysCreatesMemberWithHashedPasswordAndNormalizedOrg() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("HASHED");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(AppUser.class))).thenReturn("token");

        AuthResponse response = authService.register(new RegisterRequest("alice", "secret123", "  ACME "));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(saved.capture());
        assertEquals("HASHED", saved.getValue().getPasswordHash());
        assertEquals(Role.MEMBER, saved.getValue().getRole());
        assertEquals("acme", saved.getValue().getOrgId());
        assertEquals("token", response.token());
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(UsernameTakenException.class,
                () -> authService.register(new RegisterRequest("alice", "secret123", "acme")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        AppUser user = new AppUser("alice", "HASHED", Role.MEMBER, "acme");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "HASHED")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token");

        AuthResponse response = authService.login(new LoginRequest("alice", "secret123"));

        assertEquals("token", response.token());
        assertEquals("MEMBER", response.role());
    }

    @Test
    void loginFailsWithWrongPassword() {
        AppUser user = new AppUser("alice", "HASHED", Role.MEMBER, "acme");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASHED")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest("alice", "wrong")));
    }

    @Test
    void loginFailsForUnknownUserWithSameError() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest("ghost", "x")));
    }
}
