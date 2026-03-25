package com.hantu.identity_service.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.AccessLevel;
import com.hantu.identity_service.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.hantu.identity_service.dto.request.IntrospectTokenRequest;
import com.hantu.identity_service.dto.request.LoginRequest;
import com.hantu.identity_service.dto.request.LogoutRequest;
import com.hantu.identity_service.dto.request.RefreshTokenRequest;
import com.hantu.identity_service.dto.response.LoginResponse;
import com.hantu.identity_service.dto.response.RefreshTokenResponse;
import com.hantu.identity_service.exception.AppException;
import com.hantu.identity_service.exception.ErrorCode;
import com.hantu.identity_service.entity.InvalidatedToken;
import com.hantu.identity_service.entity.Role;
import com.hantu.identity_service.entity.User;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.hantu.identity_service.repository.InvalidatedTokenRepository;
import com.hantu.identity_service.dto.response.IntrospectResponse;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    @NonFinal
    @Value("${jwt.signer-key}")
    private String signerKey;

    @NonFinal
    @Value("${jwt.access-token-valid-duration}")
    private int accessTokenLifeTime;

    @NonFinal
    @Value("${jwt.refresh-token-valid-duration}")
    private int refreshTokenLifeTime;

    @NonFinal
    @Value("${jwt.issuer}")
    private String issuer;

    @NonFinal
    @Value("${jwt.audience}")
    private String audience;

    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;

    public LoginResponse login(LoginRequest request) {

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INCORRECT_PASSWORD);
        }

        return LoginResponse.builder()
            .accessToken(generateAccessToken(user))
            .refreshToken(generateRefreshToken(user))
            .build();

    }

    public IntrospectResponse introspect(IntrospectTokenRequest request) throws JOSEException, ParseException {
        var isValid = true;
        try {
            verifyToken(request.getAccessToken());
        } catch (JOSEException | ParseException e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
            .isValid(isValid)
            .build();
    }

    public String generateRefreshToken(User user) {

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(user.getId())
            .issuer(issuer)
            .audience(audience)
            .issueTime(new Date(Instant.now().toEpochMilli()))
            .expirationTime(new Date(Instant.now().plus(refreshTokenLifeTime, ChronoUnit.SECONDS).toEpochMilli()))
            .claim("type", "refresh")
            .jwtID(UUID.randomUUID().toString())
            .build();

        Payload payload = new Payload(claims.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey));
        } catch (JOSEException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        return jwsObject.serialize();

    }

    public String generateAccessToken(User user) {

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(user.getId())
            .issuer(issuer)
            .audience(audience)
            .issueTime(new Date(Instant.now().toEpochMilli()))
            .expirationTime(
                new Date(
                        Instant.now().plus( accessTokenLifeTime , ChronoUnit.SECONDS).toEpochMilli()))
            .claim("roles", buildScope(user))
            .claim("type", "access")
            .jwtID(UUID.randomUUID().toString())
            .build();

        Payload payload = new Payload(claims.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey));

        } catch (JOSEException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        return jwsObject.serialize();

    }

    private String buildScope(User user) {
        return user.getRoles().stream().map(Role::getName).collect(Collectors.joining(" "));
    }

    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {

        JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        var verified = signedJWT.verify(verifier);
        if (!verified) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        if (expirationTime.before(new Date())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }


        return signedJWT;

    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) 
            throws JOSEException, ParseException {

        SignedJWT signedJWT = verifyToken(request.getRefreshToken());

        var jti = signedJWT.getJWTClaimsSet().getJWTID();
        var expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        User user = userRepository.findById(signedJWT.getJWTClaimsSet().getSubject())
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
            .id(jti)
            .expirationTime(expirationTime)
            .build();
        invalidatedTokenRepository.save(invalidatedToken);

        return RefreshTokenResponse.builder()
            .accessToken(generateAccessToken(user))
            .refreshToken(generateRefreshToken(user))
            .build();
        
    }
    
    public void logout(LogoutRequest logoutRequest) throws JOSEException, ParseException {
        SignedJWT signedAccessToken = verifyToken(logoutRequest.getAccessToken());
        SignedJWT signedRefreshToken = verifyToken(logoutRequest.getRefreshToken());

        var accessTokenJti = signedAccessToken.getJWTClaimsSet().getJWTID();
        var refreshTokenJti = signedRefreshToken.getJWTClaimsSet().getJWTID();
        var accessTokenExpirationTime = signedAccessToken.getJWTClaimsSet().getExpirationTime();
        var refreshTokenExpirationTime = signedRefreshToken.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedAccessToken = InvalidatedToken.builder()
            .id(accessTokenJti)
            .expirationTime(accessTokenExpirationTime)
            .build();
        invalidatedTokenRepository.save(invalidatedAccessToken);

        InvalidatedToken invalidatedRefreshToken = InvalidatedToken.builder()
            .id(refreshTokenJti)
            .expirationTime(refreshTokenExpirationTime)
            .build();
        invalidatedTokenRepository.save(invalidatedRefreshToken);

    }

}
