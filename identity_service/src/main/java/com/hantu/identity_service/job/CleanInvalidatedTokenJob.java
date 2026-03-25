package com.hantu.identity_service.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import com.hantu.identity_service.service.InvalidatedTokenService;
import java.util.Date;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CleanInvalidatedTokenJob {
    InvalidatedTokenService invalidatedTokenService;

    @Scheduled(fixedDelay = 1000 * 60 * 60 * 24)
    public void cleanInvalidatedToken() {
        invalidatedTokenService.cleanInvalidatedToken(new Date());
    }

}
