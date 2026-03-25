package com.hantu.identity_service.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import com.hantu.identity_service.repository.UserRepository;
import com.hantu.identity_service.mapper.UserMapper;
import com.hantu.identity_service.dto.request.UserCreattionRequest;
import com.hantu.identity_service.dto.response.UserResponse;
import com.hantu.identity_service.exception.AppException;
import com.hantu.identity_service.exception.ErrorCode;
import com.hantu.identity_service.entity.User;
import java.time.LocalDateTime;
import java.util.Set;
import com.hantu.identity_service.repository.RoleRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hantu.identity_service.dto.request.UserUpdateRequest;
import com.hantu.identity_service.dto.request.ChangePasswordRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import com.hantu.identity_service.repository.httpclient.UserProfileClient;
import com.hantu.identity_service.dto.request.UserProfileCreationRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;
    UserProfileClient userProfileClient;

    @Transactional
    public UserResponse signUp(UserCreattionRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        user.setRoles(Set.of(roleRepository.findById("USER")
            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND))));

        userRepository.save(user);

        var response = userProfileClient.createUserProfile(UserProfileCreationRequest.builder()
            .userId(user.getId())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .bio(request.getBio())
            .address(request.getAddress())
            .profilePicture(request.getProfilePictureUrl())
            .gender(request.isGender())
            .dob(request.getDob())
            .build());
        
        if (response.getStatusCode().isError()) {
            throw new AppException(ErrorCode.PROFILE_CREATION_FAILED);
        }

        return userMapper.toUserResponse(user);

    }

    public UserResponse updateMe(UserUpdateRequest request) {
        var currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        final var password = request.getPassword();
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setEmail(request.getEmail());
        user.setActive(request.isActive());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    public UserResponse changeMyPassword(ChangePasswordRequest request) {
        var currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INCORRECT_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    public UserResponse getMe() {
        var currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(user);

    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
            .map(userMapper::toUserResponse)
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public UserResponse updateUserStatus(String username, boolean isActive) {
        var user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setActive(isActive);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        return userMapper.toUserResponse(userRepository.findByUsername(username)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }
}
