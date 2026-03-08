package org.poolpool.mohaeng.auth.token.refresh.repository;

import java.util.Optional;

import org.poolpool.mohaeng.auth.token.refresh.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    Optional<RefreshTokenEntity> findByUserId(Long userId);
    Optional<RefreshTokenEntity> findByUserIdAndTokenValue(Long userId, String tokenValue);
    void deleteByUserId(Long userId);
}
