package org.poolpool.mohaeng.auth.token.refresh.repository;

import java.util.Optional;

import org.poolpool.mohaeng.auth.token.refresh.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    Optional<RefreshTokenEntity> findByUserId(Long userId);
    Optional<RefreshTokenEntity> findByUserIdAndTokenValue(Long userId, String tokenValue);
    void deleteByUserId(Long userId);
    @Modifying
    @Query(value = """
        DELETE FROM refresh_token
        WHERE expires_at < NOW()
        LIMIT 1000
        """, nativeQuery = true)
    int deleteExpiredTokens();
}
