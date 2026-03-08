package org.poolpool.mohaeng.user.repository;

import java.util.Optional;

import org.poolpool.mohaeng.user.entity.SocialUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialUserRepository extends JpaRepository<SocialUserEntity, Long> {

    // 특정 소셜 계정 조회
    Optional<SocialUserEntity> findByProviderAndProviderId(String provider, String providerId);

    // 소셜 계정인지 확인
    boolean existsByUser_UserId(Long userId);

    // 소셜 계정 삭제
	void deleteByUser_UserId(Long userId);

}

