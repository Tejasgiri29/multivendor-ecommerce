package com.example.shop.service;

import com.example.shop.dto.*;
import com.example.shop.entity.Role;
import com.example.shop.entity.User;
import com.example.shop.exception.BadRequestException;
import com.example.shop.exception.ConflictException;
import com.example.shop.repository.UserRepository;
import com.example.shop.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @InjectMocks AuthService authService;

    @Test
    void registerHashesPasswordAndCreatesUnapprovedSeller() {
        when(userRepository.existsByEmailIgnoreCase("new@shop.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });
        when(jwtService.generateToken(5L, "new@shop.com", "SELLER")).thenReturn("jwt-token");

        AuthResponse res = authService.register(new RegisterRequest(" New@Shop.com ", "Secret123", "New Seller", Role.SELLER));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("HASHED", captor.getValue().getPasswordHash());
        assertNotEquals("Secret123", captor.getValue().getPasswordHash());
        assertEquals("new@shop.com", captor.getValue().getEmail());
        assertFalse(captor.getValue().isSellerApproved());
        assertEquals("jwt-token", res.token());
        assertEquals(Role.SELLER, res.user().role());
    }

    @Test
    void registerRejectsAdminRole() {
        assertThrows(BadRequestException.class,
                () -> authService.register(new RegisterRequest("a@b.com", "Secret123", "Mallory", Role.ADMIN)));
        verifyNoInteractions(userRepository);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@shop.com")).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> authService.register(new RegisterRequest("dup@shop.com", "Secret123", "Dup", Role.CUSTOMER)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenOnValidCredentials() {
        User user = TestData.user(3L, Role.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("u3@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(3L, "u3@test.com", "CUSTOMER")).thenReturn("tok");

        AuthResponse res = authService.login(new LoginRequest("u3@test.com", "pw"));

        verify(authenticationManager).authenticate(any());
        assertEquals("tok", res.token());
        assertEquals("Bearer", res.tokenType());
    }

    @Test
    void loginPropagatesBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest("x@y.com", "wrong")));
        verifyNoInteractions(jwtService);
    }

    @Test
    void bcryptEncoderProducesVerifiableNonReversibleHash() {
        PasswordEncoder enc = new BCryptPasswordEncoder();
        String hash = enc.encode("Secret123");
        assertTrue(hash.startsWith("$2"));
        assertTrue(enc.matches("Secret123", hash));
        assertFalse(enc.matches("other", hash));
    }
}
