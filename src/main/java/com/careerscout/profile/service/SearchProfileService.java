package com.careerscout.profile.service;

import com.careerscout.profile.dto.SearchProfileRequest;
import com.careerscout.profile.dto.SearchProfileResponse;
import com.careerscout.security.AuthenticatedUser;

import java.util.List;

public interface SearchProfileService {
    SearchProfileResponse create(AuthenticatedUser user, SearchProfileRequest request);
    List<SearchProfileResponse> findAll(AuthenticatedUser user);
    SearchProfileResponse findById(AuthenticatedUser user, Long profileId);
    SearchProfileResponse update(AuthenticatedUser user, Long profileId, SearchProfileRequest request);
    SearchProfileResponse updateStatus(AuthenticatedUser user, Long profileId, boolean active);
    void delete(AuthenticatedUser user, Long profileId);
}
