package org.poolpool.mohaeng.event.host.repository;

import java.util.List;

import org.poolpool.mohaeng.event.host.entity.HostBoothEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface HostBoothRepository extends JpaRepository<HostBoothEntity, Long> {
    // 행사 ID로 해당 행사의 모든 부스를 찾아오는 메서드
    List<HostBoothEntity> findByEventId(@Param("eventId") Long eventId);
}