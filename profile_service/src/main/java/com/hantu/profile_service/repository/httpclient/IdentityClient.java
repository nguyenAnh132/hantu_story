package com.hantu.profile_service.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import com.hantu.profile_service.configuration.AuthenticationRequestInterceptor;

@FeignClient(name = "identity-service", 
    url = "${app.services.identity}",
    configuration = AuthenticationRequestInterceptor.class)
public interface IdentityClient {



}
