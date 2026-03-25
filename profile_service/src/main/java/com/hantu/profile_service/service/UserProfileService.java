package com.hantu.profile_service.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.hantu.profile_service.dto.request.UserProfileCreationRequest;
import com.hantu.profile_service.dto.response.UserProfileResponse;
import com.hantu.profile_service.repository.UserProfileRepository;
import com.hantu.profile_service.entity.UserProfile;
import com.hantu.profile_service.mapper.UserProfileMapper;
import java.time.LocalDateTime;
import com.hantu.profile_service.dto.request.UserProfileUpdateRequest;
import com.hantu.profile_service.exception.AppException;
import com.hantu.profile_service.exception.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileService {

    UserProfileRepository userProfileRepository;
    UserProfileMapper userProfileMapper;
    FileStorageClientService fileStorageClientService;

    private static final int MAX_BATCH_USER_IDS = 50;

    public UserProfileResponse createUserProfile(UserProfileCreationRequest request) {

        UserProfile userProfile = userProfileMapper.toUserProfile(request);
        userProfile.setCreatedAt(LocalDateTime.now());
        userProfile.setUpdatedAt(LocalDateTime.now());
        userProfileRepository.save(userProfile);
        return userProfileMapper.toUserProfileResponse(userProfile);

    }

    public UserProfileResponse createUserProfile(UserProfileCreationRequest request, MultipartFile avatarFile) {
        if (avatarFile != null && !avatarFile.isEmpty()) {
            request.setProfilePicture(fileStorageClientService.uploadAvatar(avatarFile, currentAuthorizationHeader()));
        }
        return createUserProfile(request);
    }

    public UserProfileResponse updateMyProfile(UserProfileUpdateRequest request) {
        var userId = SecurityContextHolder.getContext().getAuthentication().getName();

        UserProfile userProfile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));

        userProfileMapper.updateUserProfile(request, userProfile);
        userProfile.setUpdatedAt(LocalDateTime.now());
        userProfileRepository.save(userProfile);

        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    public UserProfileResponse updateMyProfile(UserProfileUpdateRequest request, MultipartFile avatarFile) {
        var userId = SecurityContextHolder.getContext().getAuthentication().getName();

        UserProfile userProfile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));

        if (avatarFile != null && !avatarFile.isEmpty()) {
            String oldAvatarPath = userProfile.getProfilePicture();
            String newAvatarPath = fileStorageClientService.uploadAvatar(avatarFile, currentAuthorizationHeader());
            // Upload first to avoid losing avatar if upload fails.
            if (oldAvatarPath != null && !oldAvatarPath.isBlank()) {
                try {
                    fileStorageClientService.deleteAvatarByRelativePath(oldAvatarPath, currentAuthorizationHeader());
                } catch (AppException exception) {
                    log.warn("Failed to delete old avatar path: {}", oldAvatarPath);
                }
            }
            request.setProfilePicture(newAvatarPath);
        }

        userProfileMapper.updateUserProfile(request, userProfile);
        userProfile.setUpdatedAt(LocalDateTime.now());
        userProfileRepository.save(userProfile);

        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    public UserProfileResponse getMyProfile() {
        var userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
        return userProfileMapper.toUserProfileResponse(userProfile);
    }
    
    public UserProfileResponse getUserProfile(String userId) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    // Batch lookup for UI lists (avatar/name/bio), to avoid N x GET /{userId}.
    public List<UserProfileResponse> getUserProfilesBatch(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Sanitize input to prevent overly expensive queries and keep response stable.
        List<String> sanitized = userIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        if (sanitized.size() > MAX_BATCH_USER_IDS) {
            throw new AppException(ErrorCode.TOO_MANY_USER_IDS);
        }

        List<UserProfile> profiles = userProfileRepository.findAllByUserIds(sanitized);
        Map<String, UserProfileResponse> byUserId = profiles.stream()
                .collect(Collectors.toMap(
                        UserProfile::getUserId,
                        userProfileMapper::toUserProfileResponse,
                        (a, b) -> a
                ));

        // Preserve request order; drop missing profiles.
        return sanitized.stream()
                .map(byUserId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private String currentAuthorizationHeader() {
        var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String authHeader = requestAttributes.getRequest().getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authHeader;
    }


}
