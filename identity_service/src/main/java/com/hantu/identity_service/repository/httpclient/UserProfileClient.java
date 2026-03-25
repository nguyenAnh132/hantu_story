package com.hantu.identity_service.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import com.hantu.identity_service.dto.response.ApiResponse;
import com.hantu.identity_service.dto.response.UserProfileResponse;
import com.hantu.identity_service.dto.request.UserProfileCreationRequest;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "profile-service", url = "${app.services.profile}"
 )
public interface UserProfileClient {

    @PostMapping("/create")
    ResponseEntity<ApiResponse<UserProfileResponse>> createUserProfile(@RequestBody UserProfileCreationRequest request);

}
