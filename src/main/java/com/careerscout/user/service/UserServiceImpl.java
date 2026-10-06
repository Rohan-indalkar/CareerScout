package com.careerscout.user.service;

import com.careerscout.auth.service.AuthService;
import com.careerscout.common.exception.BadRequestException;
import com.careerscout.common.exception.DuplicateResourceException;
import com.careerscout.common.exception.ResourceNotFoundException;
import com.careerscout.security.AuthenticatedUser;
import com.careerscout.user.UserAccount;
import com.careerscout.user.UserRepository;
import com.careerscout.user.dto.ChangePasswordRequest;
import com.careerscout.user.dto.UpdateProfileRequest;
import com.careerscout.user.dto.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserServiceImpl(UserRepository users, PasswordEncoder passwordEncoder, AuthService authService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(AuthenticatedUser principal) {
        return toResponse(findUser(principal.id()));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(AuthenticatedUser principal, UpdateProfileRequest request) {
        UserAccount user = findUser(principal.id());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!user.getEmail().equalsIgnoreCase(email) && users.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        user.updateProfile(request.name().trim(), email);
        try {
            return toResponse(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
    }

    @Override
    @Transactional
    public void changePassword(AuthenticatedUser principal, ChangePasswordRequest request) {
        UserAccount user = findUser(principal.id());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Password must be no more than 72 UTF-8 bytes");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        authService.revokeSessions(user.getId());
    }

    private UserAccount findUser(Long id) {
        return users.findById(id).filter(UserAccount::isEnabled)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
    }

    private static UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
