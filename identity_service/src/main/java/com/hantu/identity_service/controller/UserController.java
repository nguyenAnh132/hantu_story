package com.hantu.identity_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.hantu.identity_service.service.UserService;
import com.hantu.identity_service.dto.response.ApiResponse;
import com.hantu.identity_service.dto.response.UserResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.hantu.identity_service.dto.request.UserCreattionRequest;
import com.hantu.identity_service.dto.request.UserUpdateRequest;
import com.hantu.identity_service.dto.request.ChangePasswordRequest;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {

    UserService userService;

    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponse<UserResponse>> signUp(@RequestBody @Valid UserCreattionRequest userCreattionRequest) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
            .code(1000)
            .message("User signed up successfully")
            .result(userService.signUp(userCreattionRequest))
            .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
            .code(1000)
            .message("User retrieved successfully")
            .result(userService.getMe())
            .build());
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(@RequestBody @Valid UserUpdateRequest userUpdateRequest) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
            .code(1000)
            .message("User updated successfully")
            .result(userService.updateMe(userUpdateRequest))
            .build());
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<UserResponse>> changeMyPassword(
            @RequestBody @Valid ChangePasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
            .code(1000)
            .message("Password changed successfully")
            .result(userService.changeMyPassword(request))
            .build());
    }

    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
            .code(1000)
            .message("User retrieved successfully")
            .result(userService.getUserByUsername(username))
            .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
            .code(1000)
            .message("Users retrieved successfully")
            .result(userService.getUsers())
            .build());
    }
    
    @PutMapping("/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(@PathVariable String username, boolean isActive) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
            .code(1000)
            .message("User updated successfully")
            .result(userService.updateUserStatus(username, isActive))
            .build());
    }

}
