package com.careerscout.profile.service;

import com.careerscout.common.exception.DuplicateResourceException;
import com.careerscout.common.exception.ResourceNotFoundException;
import com.careerscout.profile.SearchProfile;
import com.careerscout.profile.SearchProfileRepository;
import com.careerscout.profile.dto.SearchProfileRequest;
import com.careerscout.profile.dto.SearchProfileResponse;
import com.careerscout.security.AuthenticatedUser;
import com.careerscout.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SearchProfileServiceImpl implements SearchProfileService {
    private final SearchProfileRepository profiles;
    private final UserRepository users;

    public SearchProfileServiceImpl(SearchProfileRepository profiles, UserRepository users) {
        this.profiles = profiles;
        this.users = users;
    }

    @Override
    @Transactional
    public SearchProfileResponse create(AuthenticatedUser principal, SearchProfileRequest request) {
        var owner = users.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
        String name = request.name().trim();
        String normalizedName = normalizeName(name);
        if (profiles.existsByUserIdAndNormalizedName(principal.id(), normalizedName)) {
            throw new DuplicateResourceException("A search profile with this name already exists");
        }
        SearchProfile profile = new SearchProfile(owner, name, normalizedName, request.position().trim(),
                request.location().trim(), request.experienceLevel(), normalizeTerms(request.skills()),
                normalizeTerms(request.keywords()));
        return save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchProfileResponse> findAll(AuthenticatedUser principal) {
        return profiles.findAllByUserIdOrderByCreatedAtDesc(principal.id()).stream()
                .map(SearchProfileServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SearchProfileResponse findById(AuthenticatedUser principal, Long profileId) {
        return toResponse(findOwnedProfile(principal.id(), profileId));
    }

    @Override
    @Transactional
    public SearchProfileResponse update(AuthenticatedUser principal, Long profileId,
                                        SearchProfileRequest request) {
        SearchProfile profile = findOwnedProfile(principal.id(), profileId);
        String name = request.name().trim();
        String normalizedName = normalizeName(name);
        if (profiles.existsByUserIdAndNormalizedNameAndIdNot(principal.id(), normalizedName, profileId)) {
            throw new DuplicateResourceException("A search profile with this name already exists");
        }
        profile.update(name, normalizedName, request.position().trim(), request.location().trim(),
                request.experienceLevel(), normalizeTerms(request.skills()), normalizeTerms(request.keywords()));
        return save(profile);
    }

    @Override
    @Transactional
    public SearchProfileResponse updateStatus(AuthenticatedUser principal, Long profileId, boolean active) {
        SearchProfile profile = findOwnedProfile(principal.id(), profileId);
        profile.setActive(active);
        return save(profile);
    }

    @Override
    @Transactional
    public void delete(AuthenticatedUser principal, Long profileId) {
        profiles.delete(findOwnedProfile(principal.id(), profileId));
    }

    private SearchProfile findOwnedProfile(Long userId, Long profileId) {
        return profiles.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Search profile not found"));
    }

    private SearchProfileResponse save(SearchProfile profile) {
        try {
            return toResponse(profiles.saveAndFlush(profile));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("A search profile with this name already exists");
        }
    }

    private static SearchProfileResponse toResponse(SearchProfile profile) {
        return new SearchProfileResponse(profile.getId(), profile.getName(), profile.getPosition(),
                profile.getLocation(), profile.getExperienceLevel(),
                profile.getSkills().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                profile.getKeywords().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                profile.isActive(), profile.getCreatedAt(), profile.getUpdatedAt());
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static Set<String> normalizeTerms(List<String> terms) {
        if (terms == null) return Set.of();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String term : terms) {
            String trimmed = term.trim().replaceAll("\\s+", " ");
            if (!trimmed.isEmpty()) normalized.add(trimmed);
        }
        return normalized;
    }
}
