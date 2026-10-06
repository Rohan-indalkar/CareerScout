package com.careerscout.career.service;

import com.careerscout.career.dto.CareerSourceRequest;
import com.careerscout.career.dto.CareerSourceResponse;
import com.careerscout.security.AuthenticatedUser;

import java.util.List;

public interface CareerSourceService {
    CareerSourceResponse create(AuthenticatedUser user, CareerSourceRequest request);
    List<CareerSourceResponse> findAll(AuthenticatedUser user);
    CareerSourceResponse findById(AuthenticatedUser user, Long sourceId);
    CareerSourceResponse update(AuthenticatedUser user, Long sourceId, CareerSourceRequest request);
    CareerSourceResponse updateStatus(AuthenticatedUser user, Long sourceId, boolean active);
    void delete(AuthenticatedUser user, Long sourceId);
}
