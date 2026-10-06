package com.careerscout.user.service;

import com.careerscout.security.AuthenticatedUser;
import com.careerscout.user.dto.ChangePasswordRequest;
import com.careerscout.user.dto.UpdateProfileRequest;
import com.careerscout.user.dto.UserResponse;

public interface UserService {
    UserResponse getCurrentUser(AuthenticatedUser principal);
    UserResponse updateProfile(AuthenticatedUser principal, UpdateProfileRequest request);
    void changePassword(AuthenticatedUser principal, ChangePasswordRequest request);
}
