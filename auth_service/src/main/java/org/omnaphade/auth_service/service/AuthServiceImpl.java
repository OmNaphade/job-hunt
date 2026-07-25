package org.omnaphade.auth_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.omnaphade.auth_service.client.UserServiceClient;
import org.omnaphade.auth_service.dtos.*;
import org.omnaphade.auth_service.entities.PasswordResetToken;
import org.omnaphade.auth_service.entities.Role;
import org.omnaphade.auth_service.entities.User;
import org.omnaphade.auth_service.exception.DuplicateResourceException;
import org.omnaphade.auth_service.exception.ResourceNotFoundException;
import org.omnaphade.auth_service.exception.AuthenticationFailedException;
import org.omnaphade.auth_service.repository.AuthRepository;
import org.omnaphade.auth_service.repository.PasswordResetTokenRepository;
import org.omnaphade.auth_service.repository.RefreshTokenRepository;
import org.omnaphade.auth_service.entities.RefreshToken;
import org.omnaphade.auth_service.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements IAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String USER_NOT_FOUND = "User not found";

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserServiceClient userServiceClient;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    // ================= REGISTER =================
    @Override
    public AuthResponseDTO registerUser(RegisterRequestDTO registerRequest) {

        if (authRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        Role requestedRole;
        try {
            requestedRole = Role.valueOf(registerRequest.getRole().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid role. Allowed roles: JOB_SEEKER, RECRUITER");
        }

        if (requestedRole == Role.ADMIN) {
            throw new IllegalArgumentException("Self-registration for ADMIN is not allowed");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(requestedRole);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        User savedUser = authRepository.save(user);

        // 🔥 CALL USER SERVICE (JSON based)
        try {
            userServiceClient.createUserProfile(
                    savedUser.getId(),
                    "New User",
                    "Profile created automatically"
            );
        } catch (Exception e) {
            // Don't break auth flow if user-service fails
            log.warn("UserService call failed: {}", e.getMessage());
        }

        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                savedUser.getRole().name(),
                savedUser.getId()
        );
    }

    // ================= LOGIN =================
    @Override
    public AuthResponseDTO loginUser(LoginRequestDTO loginRequest) {

        User user = authRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found. Please register as JOB_SEEKER or RECRUITER"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                user.getRole().name(),
                user.getId()
        );
    }

    // ================= REFRESH TOKEN =================
    @Override
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest) {

        String refreshToken = refreshTokenRequest.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new ResourceNotFoundException("Invalid refresh token");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        User user = authRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new AuthResponseDTO(
                newAccessToken,
                newRefreshToken,
                user.getRole().name(),
                user.getId()
        );
    }

    // ================= GET USER =================
    @Override
    public UserResponseDTO getUserDetails(Long userId) {

        User user = authRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus()
        );
    }

    // ================= UPDATE PASSWORD =================
    @Override
    public void updatePassword(Long userId, PasswordDTO passwordDTO) {

        User user = authRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (!passwordEncoder.matches(passwordDTO.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(passwordDTO.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        authRepository.save(user);
    }

    @Override
    public void requestPasswordReset(PasswordResetRequestDTO requestDTO) {
        authRepository.findByEmail(requestDTO.getEmail()).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUserId(user.getId());

            String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(sha256(rawToken))
                    .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30))
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(token);

            // Replace this with email/SMS dispatch integration in production.
            log.info("Password reset token for {}: {}", user.getEmail(), rawToken);
        });
    }

    @Override
    public void resetPassword(PasswordResetConfirmDTO confirmDTO) {
        PasswordResetToken token = passwordResetTokenRepository
            .findByTokenHashAndUsedFalseAndExpiresAtAfter(sha256(confirmDTO.getToken()), LocalDateTime.now(ZoneOffset.UTC))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token"));

        User user = authRepository.findById(token.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(confirmDTO.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        authRepository.save(user);

        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now(ZoneOffset.UTC));
        passwordResetTokenRepository.save(token);

        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    // ================= DELETE USER =================
    @Override
    public void deleteUser(Long userId) {

        User user = authRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        authRepository.delete(user);

        // Optional: also delete from user-service
        try {
            userServiceClient.deleteUserProfile(userId);
        } catch (Exception e) {
            log.warn("UserService delete failed: {}", e.getMessage());
        }
    }

    // ================= LOGOUT =================
    @Override
    public void logout(String refreshToken) {
        String tokenHash = sha256(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    // ================= HELPERS =================
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
