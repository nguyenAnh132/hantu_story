package com.hantu.profile_service.controller;

import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import com.hantu.profile_service.service.UserProfileService;
import com.hantu.profile_service.dto.request.UserProfileCreationRequest;
import com.hantu.profile_service.dto.response.UserProfileResponse;
import com.hantu.profile_service.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;    
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import jakarta.validation.Valid;
import com.hantu.profile_service.dto.request.UserProfileUpdateRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileController {
    UserProfileService userProfileService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUserProfile(@RequestBody @Valid UserProfileCreationRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
            .message("User profile created successfully")
            .result(userProfileService.createUserProfile(request))
            .build());
    }

    @PostMapping(value = "/create-with-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUserProfileWithAvatar(
            @Valid @ModelAttribute UserProfileCreationRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .message("User profile created successfully")
                .result(userProfileService.createUserProfile(request, avatar))
                .build());
    }

    @PutMapping("/update/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(@RequestBody @Valid UserProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
            .message("User profile updated successfully")
            .result(userProfileService.updateMyProfile(request))
            .build());
    }

    @PutMapping(value = "/update/me-with-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfileWithAvatar(
            @Valid @ModelAttribute UserProfileUpdateRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .message("User profile updated successfully")
                .result(userProfileService.updateMyProfile(request, avatar))
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
            .message("User profile retrieved successfully")
            .result(userProfileService.getMyProfile())
            .build());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
            .message("User profile retrieved successfully")
            .result(userProfileService.getUserProfile(userId))
            .build());
    }

    // Batch lookup for UI lists (avatar/name/bio), to avoid N x GET /{userId}.
    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getUserProfilesBatch(
            @RequestParam("userIds") String userIds) {
        List<String> ids = Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<UserProfileResponse>>builder()
                .message("User profiles batch retrieved successfully")
                .result(userProfileService.getUserProfilesBatch(ids))
                .build());
    }

}
