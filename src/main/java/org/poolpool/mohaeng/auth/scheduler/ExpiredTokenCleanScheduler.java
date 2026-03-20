package org.poolpool.mohaeng.auth.scheduler;

import org.poolpool.mohaeng.auth.token.refresh.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredTokenCleanScheduler {

	private final RefreshTokenRepository tokenRepository;

	    private static final int BATCH_SIZE = 1000;

	    @Scheduled(cron = "0 5 0 * * *")
	    @Transactional
	    public void cleanupExpiredTokens() {
	        int totalDeleted = 0;

	        while (true) {
	            int deleted = tokenRepository.deleteExpiredTokens();
	            totalDeleted += deleted;

	            if (deleted < BATCH_SIZE) {
	                break;
	            }
	        }

	        log.info("ExpiredTokenCleanScheduler - deleted {} tokens", totalDeleted);
	    }
}
