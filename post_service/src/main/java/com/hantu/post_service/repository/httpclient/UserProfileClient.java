package com.hantu.post_service.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import com.hantu.post_service.configuration.AuthenticationRequestInterceptor;
import com.hantu.post_service.dto.response.ApiResponse;
import com.hantu.post_service.dto.response.UserProfileResponse;

@FeignClient(
        name = "profile-service",
        contextId = "profileServiceUserProfileClient",
        url = "${app.services.profile}",
        configuration = AuthenticationRequestInterceptor.class
)
public interface UserProfileClient {

    @GetMapping("/{userId}")
    ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable String userId);
}

