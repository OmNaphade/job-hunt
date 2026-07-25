package org.omnaphade.auth_service.service;

import org.omnaphade.auth_service.dtos.*;

public interface IAuthService {

    AuthResponseDTO registerUser(RegisterRequestDTO registerRequest);

    AuthResponseDTO loginUser(LoginRequestDTO loginRequest);

    AuthResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest);

    UserResponseDTO getUserDetails(Long userId);

    void updatePassword(Long userId, PasswordDTO passwordDTO);

    void requestPasswordReset(PasswordResetRequestDTO requestDTO);

    void resetPassword(PasswordResetConfirmDTO confirmDTO);

    void deleteUser(Long userId);

    void logout(String refreshToken);
}
