package org.poolpool.mohaeng.auth.token.refresh.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.poolpool.mohaeng.auth.token.refresh.entity.RefreshTokenEntity;
import org.poolpool.mohaeng.auth.token.refresh.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    @Transactional
    public RefreshTokenEntity upsert(Long userId, String tokenValue, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        
    	
    	RefreshTokenEntity entity = repo.findByUserId(userId)
                .orElseGet(() -> RefreshTokenEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .build());

        entity.setTokenValue(tokenValue);
        entity.setIssuedAt(issuedAt);
        entity.setExpiresAt(expiresAt);
        entity.setRevoked(false);

        return repo.save(entity);
    }

    @Transactional(readOnly = true)
    public boolean matches(Long userId, String tokenValue) {
        return repo.findByUserIdAndTokenValue(userId, tokenValue).isPresent();
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        repo.deleteByUserId(userId);
    }
}
