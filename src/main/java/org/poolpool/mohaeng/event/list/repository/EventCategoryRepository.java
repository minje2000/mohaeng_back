package org.poolpool.mohaeng.event.list.repository;

import org.poolpool.mohaeng.event.list.entity.EventCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCategoryRepository extends JpaRepository<EventCategoryEntity, Integer> {
    // 기본 CRUD 메서드가 자동으로 생성됩니다.
}