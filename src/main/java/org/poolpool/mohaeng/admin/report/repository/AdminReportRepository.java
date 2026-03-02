package org.poolpool.mohaeng.admin.report.repository;

import java.util.List;

import org.poolpool.mohaeng.admin.report.entity.AdminReportFEntity;
import org.poolpool.mohaeng.admin.report.type.ReportResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AdminReportRepository extends JpaRepository<AdminReportFEntity, Long> {

    // 최신순 목록
    Page<AdminReportFEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 중복 신고 방지
    boolean existsByReporterIdAndEventId(long reporterId, long eventId);

    // 같은 이벤트의 다른 신고들 삭제
    int deleteByEventIdAndReportIdNot(long eventId, long reportId);

    //  FK 끊기용: eventId에 해당하는 신고 reportId 전체 조회
    @Query("select r.reportId from AdminReportFEntity r where r.eventId = :eventId")
    List<Long> findReportIdsByEventId(@Param("eventId") Long eventId);
}