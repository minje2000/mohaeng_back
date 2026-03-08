package org.poolpool.mohaeng.event.list.repository;

import org.poolpool.mohaeng.event.list.entity.EventRegionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegionRepository extends JpaRepository<EventRegionEntity, Long> {
    // 기본 CRUD 메서드가 자동으로 생성됩니다.
}