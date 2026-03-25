package com.hantu.identity_service.controller;

import java.text.ParseException;
import java.time.Duration;
import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.AccessLevel;
import com.hantu.identity_service.service.AuthenticationService;
import com.hantu.identity_service.dto.response.ApiResponse;
import com.hantu.identity_service.dto.response.LoginResponse;
import com.hantu.identity_service.dto.request.IntrospectTokenRequest;
import com.hantu.identity_service.dto.request.LoginRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import com.hantu.identity_service.dto.request.LogoutRequest;
import com.hantu.identity_service.dto.request.RefreshTokenRequest;
import com.nimbusds.jose.JOSEException;
import com.hantu.identity_service.dto.response.RefreshTokenResponse;
import com.hantu.identity_service.dto.response.IntrospectResponse;

import jakarta.servlet.http.Cookie;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationController {
    AuthenticationService authenticationService;

    @Value("${jwt.access-token-valid-duration}")
    @NonFinal int accessTokenLifeTime;

    @Value("${jwt.refresh-token-valid-duration}")
    @NonFinal int refreshTokenLifeTime;

    static final String ACCESS_COOKIE_NAME = "accessToken";
    static final String REFRESH_COOKIE_NAME = "refreshToken";
    static final String COOKIE_PATH = "/api";

    @PostMapping("/sign-in")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
        @RequestBody @Valid LoginRequest loginRequest,
        HttpServletRequest request
    ) {
        LoginResponse loginResponse = authenticationService.login(loginRequest);

        ResponseCookie accessCookie = buildAccessTokenCookie(loginResponse.getAccessToken(), request);
        ResponseCookie refreshCookie = buildRefreshTokenCookie(loginResponse.getRefreshToken(), request);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(ApiResponse.<LoginResponse>builder()
                .code(1000)
                .message("Login successful")
                .result(loginResponse)
                .build());
    }


    @PostMapping("/refresh-cookie")
    public ResponseEntity<ApiResponse<Void>> refreshCookie(
        HttpServletRequest request
    ) throws JOSEException, ParseException {
        String refreshToken = getCookieValue(request, REFRESH_COOKIE_NAME);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Void>builder()
                    .code(1401)
                    .message("Unauthenticated")
                    .build());
        }

        RefreshTokenResponse refreshed = authenticationService.refreshToken(
            RefreshTokenRequest.builder().refreshToken(refreshToken).build()
        );

        ResponseCookie accessCookie = buildAccessTokenCookie(refreshed.getAccessToken(), request);
        ResponseCookie refreshCookie = buildRefreshTokenCookie(refreshed.getRefreshToken(), request);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(ApiResponse.<Void>builder()
                .code(1000)
                .message("Refresh successful")
                .build());
    }

    @PostMapping("/logout-cookie")
    public ResponseEntity<ApiResponse<Void>> logoutCookie(HttpServletRequest request)
        throws JOSEException, ParseException {
        String accessToken = getCookieValue(request, ACCESS_COOKIE_NAME);
        String refreshToken = getCookieValue(request, REFRESH_COOKIE_NAME);
        if (accessToken != null && refreshToken != null
            && !accessToken.isBlank() && !refreshToken.isBlank()) {
            authenticationService.logout(LogoutRequest.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build());
        }

        ResponseCookie accessCookie = clearAccessTokenCookie(request);
        ResponseCookie refreshCookie = clearRefreshTokenCookie(request);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(ApiResponse.<Void>builder()
                .code(1000)
                .message("Logout successful")
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody @Valid LogoutRequest logoutRequest) 
            throws JOSEException, ParseException {

        authenticationService.logout(logoutRequest);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .code(1000)
            .message("Logout successful")
            .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request) 
        throws JOSEException, ParseException {
        return ResponseEntity.ok(ApiResponse.<RefreshTokenResponse>builder()
            .code(1000)
            .message("Refresh successful")
            .result(authenticationService.refreshToken(request))
            .build());
    }

    @PostMapping("/introspect")
    public ResponseEntity<ApiResponse<IntrospectResponse>> introspect(@RequestBody @Valid IntrospectTokenRequest request) 
        throws JOSEException, ParseException {
        return ResponseEntity.ok(ApiResponse.<IntrospectResponse>builder()
            .code(1000)
            .message("Introspect successful")
            .result(authenticationService.introspect(request))
            .build());
    }

    private boolean isSecure(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null) {
            return "https".equalsIgnoreCase(forwardedProto);
        }
        return request.isSecure();
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
            .filter(c -> cookieName.equals(c.getName()))
            .findFirst()
            .map(Cookie::getValue)
            .orElse(null);
    }

    private ResponseCookie buildAccessTokenCookie(String token, HttpServletRequest request) {
        return ResponseCookie.from(ACCESS_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(isSecure(request))
            .sameSite("Lax")
            .path(COOKIE_PATH)
            .maxAge(Duration.ofSeconds(accessTokenLifeTime))
            .build();
    }

    private ResponseCookie buildRefreshTokenCookie(String token, HttpServletRequest request) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(isSecure(request))
            .sameSite("Lax")
            .path(COOKIE_PATH)
            .maxAge(Duration.ofSeconds(refreshTokenLifeTime))
            .build();
    }

    private ResponseCookie clearAccessTokenCookie(HttpServletRequest request) {
        return ResponseCookie.from(ACCESS_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(isSecure(request))
            .sameSite("Lax")
            .path(COOKIE_PATH)
            .maxAge(Duration.ZERO)
            .build();
    }

    private ResponseCookie clearRefreshTokenCookie(HttpServletRequest request) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(isSecure(request))
            .sameSite("Lax")
            .path(COOKIE_PATH)
            .maxAge(Duration.ZERO)
            .build();
    }
}
