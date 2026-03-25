package com.hantu.identity_service.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import com.hantu.identity_service.repository.InvalidatedTokenRepository;
import com.hantu.identity_service.entity.InvalidatedToken;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InvalidatedTokenService {
    InvalidatedTokenRepository invalidatedTokenRepository;

    public void cleanInvalidatedToken(Date expirationTime) {
        List<InvalidatedToken> invalidatedTokens = invalidatedTokenRepository.findAllByExpirationTimeBefore(expirationTime);
        invalidatedTokenRepository.deleteAll(invalidatedTokens);
    }
}
