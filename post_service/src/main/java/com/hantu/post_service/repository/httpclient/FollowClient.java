package com.hantu.post_service.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.hantu.post_service.configuration.AuthenticationRequestInterceptor;
import com.hantu.post_service.dto.response.ApiResponse;
import com.hantu.post_service.dto.response.FollowingPageResponse;

@FeignClient(
        name = "profile-service",
        url = "${app.services.profile}",
        contextId = "profileServiceFollowClient",
        configuration = AuthenticationRequestInterceptor.class
)
public interface FollowClient {

    @PostMapping("/follows/{targetUserId}")
    ApiResponse<Void> follow(@PathVariable String targetUserId);

    @DeleteMapping("/follows/{targetUserId}")
    ApiResponse<Void> unfollow(@PathVariable String targetUserId);

    @GetMapping("/follows/me/following")
    ApiResponse<FollowingPageResponse> getFollowing(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "10") int size
    );
}

