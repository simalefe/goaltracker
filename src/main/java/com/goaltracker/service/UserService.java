package com.goaltracker.service;

import com.goaltracker.dto.request.ChangePasswordRequest;
import com.goaltracker.dto.request.UpdateProfileRequest;
import com.goaltracker.dto.response.UserResponse;
import com.goaltracker.exception.InvalidCredentialsException;
import com.goaltracker.mapper.UserMapper;
import com.goaltracker.model.User;
import com.goaltracker.repository.RefreshTokenRepository;
import com.goaltracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getProfile(String email) {
        User user = findUserByEmail(email);
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUserByEmail(email);
        if (request.displayName() != null) user.setDisplayName(request.displayName());
        if (request.timezone() != null) user.setTimezone(request.timezone());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        user = userRepository.save(user);
        log.info("Profil güncellendi: email={}", email);
        return UserMapper.toResponse(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findUserByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Şifre değiştirildi: email={}", email);
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = findUserByEmail(email);
        user.setActive(false);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Hesap devre dışı bırakıldı: email={}", email);
    }

    public List<UserResponse> searchUsers(String query) {
        return userRepository.findByUsernameContainingIgnoreCaseAndIsActiveTrue(query)
                .stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }
}

