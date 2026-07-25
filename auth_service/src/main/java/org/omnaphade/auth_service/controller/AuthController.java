package org.omnaphade.auth_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.omnaphade.auth_service.dtos.*;
import org.omnaphade.auth_service.service.IAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    // ================= REGISTER =================
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> registerUser(
            @Valid @RequestBody RegisterRequestDTO registerRequest) {

        return ResponseEntity.ok(authService.registerUser(registerRequest));
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> loginUser(
            @Valid @RequestBody LoginRequestDTO loginRequest) {

        return ResponseEntity.ok(authService.loginUser(loginRequest));
    }

    // ================= REFRESH TOKEN =================
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequest) {

        return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest));
    }

    // ================= GET USER =================
    @GetMapping("/users/{userId}")
    @PreAuthorize("authentication.name == #userId.toString() or hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserDetails(
            @PathVariable Long userId) {

        return ResponseEntity.ok(authService.getUserDetails(userId));
    }

    // ================= UPDATE PASSWORD =================
    @PutMapping("/users/{userId}/password")
    @PreAuthorize("authentication.name == #userId.toString() or hasRole('ADMIN')")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody PasswordDTO passwordDTO) {

        authService.updatePassword(userId, passwordDTO);
        return ResponseEntity.noContent().build();
    }

    // ================= PASSWORD RESET (FORGOT PASSWORD) =================
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO requestDTO) {
        authService.requestPasswordReset(requestDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDTO confirmDTO) {
        authService.resetPassword(confirmDTO);
        return ResponseEntity.noContent().build();
    }

    // ================= DELETE USER =================
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId) {

        authService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // ================= LOGOUT =================
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequestDTO request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    // ================= CURRENT USER =================
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(authService.getUserDetails(userId));
    }
}
