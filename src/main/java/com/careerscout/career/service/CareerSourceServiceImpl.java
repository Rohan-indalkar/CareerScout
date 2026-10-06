package com.careerscout.career.service;

import com.careerscout.career.dto.CareerSourceRequest;
import com.careerscout.career.dto.CareerSourceResponse;
import com.careerscout.career.entity.CareerSource;
import com.careerscout.career.repository.CareerSourceRepository;
import com.careerscout.common.exception.DuplicateResourceException;
import com.careerscout.common.exception.ResourceNotFoundException;
import com.careerscout.security.AuthenticatedUser;
import com.careerscout.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CareerSourceServiceImpl implements CareerSourceService {
    private final CareerSourceRepository careerSources;
    private final UserRepository users;
    private final CareerUrlValidator urlValidator;

    public CareerSourceServiceImpl(CareerSourceRepository careerSources, UserRepository users,
                                   CareerUrlValidator urlValidator) {
        this.careerSources = careerSources;
        this.users = users;
        this.urlValidator = urlValidator;
    }

    @Override
    @Transactional
    public CareerSourceResponse create(AuthenticatedUser principal, CareerSourceRequest request) {
        var owner = users.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
        String normalizedUrl = urlValidator.normalize(request.careerUrl());
        if (careerSources.existsByUserIdAndNormalizedUrl(owner.getId(), normalizedUrl)) {
            throw new DuplicateResourceException("This career page is already being monitored");
        }
        CareerSource source = new CareerSource(owner, request.companyName().trim(), normalizedUrl,
                normalizedUrl, request.scanIntervalMinutes());
        return save(source);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CareerSourceResponse> findAll(AuthenticatedUser principal) {
        return careerSources.findAllByUserIdOrderByCreatedAtDesc(principal.id())
                .stream().map(CareerSourceServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CareerSourceResponse findById(AuthenticatedUser principal, Long sourceId) {
        return toResponse(findOwnedSource(principal.id(), sourceId));
    }

    @Override
    @Transactional
    public CareerSourceResponse update(AuthenticatedUser principal, Long sourceId, CareerSourceRequest request) {
        CareerSource source = findOwnedSource(principal.id(), sourceId);
        String normalizedUrl = urlValidator.normalize(request.careerUrl());
        if (careerSources.existsByUserIdAndNormalizedUrlAndIdNot(principal.id(), normalizedUrl, sourceId)) {
            throw new DuplicateResourceException("This career page is already being monitored");
        }
        source.update(request.companyName().trim(), normalizedUrl, normalizedUrl, request.scanIntervalMinutes());
        return save(source);
    }

    @Override
    @Transactional
    public CareerSourceResponse updateStatus(AuthenticatedUser principal, Long sourceId, boolean active) {
        CareerSource source = findOwnedSource(principal.id(), sourceId);
        source.setActive(active);
        return save(source);
    }

    @Override
    @Transactional
    public void delete(AuthenticatedUser principal, Long sourceId) {
        CareerSource source = findOwnedSource(principal.id(), sourceId);
        careerSources.delete(source);
    }

    private CareerSource findOwnedSource(Long ownerId, Long sourceId) {
        return careerSources.findByIdAndUserId(sourceId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Career source not found"));
    }

    private CareerSourceResponse save(CareerSource source) {
        try {
            return toResponse(careerSources.saveAndFlush(source));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("This career page is already being monitored");
        }
    }

    private static CareerSourceResponse toResponse(CareerSource source) {
        return new CareerSourceResponse(source.getId(), source.getCompanyName(), source.getCareerUrl(),
                source.isActive(), source.getScanIntervalMinutes(), source.getLastScannedAt(),
                source.getCreatedAt(), source.getUpdatedAt());
    }
}
