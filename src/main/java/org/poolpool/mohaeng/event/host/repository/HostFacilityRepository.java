package org.poolpool.mohaeng.event.host.repository;

import java.util.List;

import org.poolpool.mohaeng.event.host.entity.HostFacilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface HostFacilityRepository extends JpaRepository<HostFacilityEntity, Long> {
    // 행사 ID로 해당 행사의 모든 부대시설을 찾아오는 메서드
    List<HostFacilityEntity> findByEventId(@Param("eventId") Long eventId);
}