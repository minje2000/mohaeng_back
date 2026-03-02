package org.poolpool.mohaeng.admin.report.service;

import java.util.List;

import org.poolpool.mohaeng.admin.report.dto.AdminReportCreateRequestDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportDetailDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportListItemDto;
import org.poolpool.mohaeng.admin.report.entity.AdminReportFEntity;
import org.poolpool.mohaeng.admin.report.repository.AdminReportRepository;
import org.poolpool.mohaeng.admin.report.type.ReportResult;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.notification.repository.NotificationRepository; //  추가
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.type.NotiTypeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final AdminReportRepository reportRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;

    //  FK 끊기용
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminReportListItemDto> getList(Pageable pageable) {
        Page<AdminReportFEntity> page = reportRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<AdminReportListItemDto> items = page.getContent().stream()
            .map(r -> AdminReportListItemDto.fromEntity(r, getEventNameSafe(r.getEventId())))
            .toList();

        return new PageResponse<>(items, pageable.getPageNumber(), pageable.getPageSize(),
            page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReportDetailDto getDetail(long reportId) {
        AdminReportFEntity r = reportRepository.findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException("신고가 존재하지 않습니다."));

        return AdminReportDetailDto.fromEntity(r, getEventNameSafe(r.getEventId()));
    }

    @Override
    @Transactional
    public long create(long reporterId, AdminReportCreateRequestDto request) {
        Long eventId = request.getEventId();

        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("존재하지 않는 이벤트입니다.");
        }

        if (reportRepository.existsByReporterIdAndEventId(reporterId, eventId)) {
            throw new IllegalStateException("이미 해당 이벤트를 신고했습니다.");
        }

        AdminReportFEntity r = AdminReportFEntity.builder()
            .eventId(eventId)
            .reporterId(reporterId)
            .reasonCategory(request.getReasonCategory())
            .reasonDetailText(request.getReasonDetailText())
            .reportResult(ReportResult.PENDING)
            .build();

        return reportRepository.save(r).getReportId();
    }

    @Override
    @Transactional
    public void approve(long reportId) {
        AdminReportFEntity r = reportRepository.findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException("신고가 존재하지 않습니다."));

        if (!ReportResult.PENDING.equals(r.getReportResult())) {
            throw new IllegalStateException("이미 처리된 신고입니다.");
        }

        // 이벤트 조회(주최자 알림/상태 변경에 필요)
        EventEntity event = eventRepository.findById(r.getEventId())
            .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 이벤트입니다."));

        // 1) 신고자에게 승인 알림(6)
        notificationService.create(r.getReporterId(), NotiTypeId.REPORT_ACCEPT, r.getEventId(), null);

        // 2) 주최자에게 “신고 승인(행사 제재)” 알림(5)
        if (event.getHost() != null && event.getHost().getUserId() != null) {
            long hostUserId = event.getHost().getUserId();
            if (hostUserId != r.getReporterId()) {
                notificationService.create(hostUserId, NotiTypeId.REPORT_RECEIVER, r.getEventId(), null);
            }
        }

        // 3) 이벤트 비활성화
        event.changeStatusToDeleted();

        //  4) (가장 중요) 같은 이벤트의 "모든 신고 reportId"에 대해 notification.reportId FK 끊기
        List<Long> reportIds = reportRepository.findReportIdsByEventId(r.getEventId());
        if (reportIds != null && !reportIds.isEmpty()) {
            notificationRepository.detachReports(reportIds);
        }

        // 5) 같은 이벤트의 다른 신고 삭제
        reportRepository.deleteByEventIdAndReportIdNot(r.getEventId(), r.getReportId());

        // 6) 현재 신고도 삭제
        reportRepository.delete(r);
    }

    @Override
    @Transactional
    public void reject(long reportId) {
        AdminReportFEntity r = reportRepository.findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException("신고가 존재하지 않습니다."));

        if (!ReportResult.PENDING.equals(r.getReportResult())) {
            throw new IllegalStateException("이미 처리된 신고입니다.");
        }

        // 1) 신고자에게 반려 알림(7)
        notificationService.create(r.getReporterId(), NotiTypeId.REPORT_REJECT, r.getEventId(), null);

        //  2) (중요) 현재 신고 reportId에 대해 FK 끊기
        notificationRepository.detachReport(r.getReportId());

        // 3) 현재 신고 삭제
        reportRepository.delete(r);
    }

    private String getEventNameSafe(Long eventId) {
        return eventRepository.findById(eventId)
            .map(EventEntity::getTitle)
            .orElse("(삭제/미존재 이벤트)");
    }
}