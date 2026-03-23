package org.poolpool.mohaeng.notification.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.poolpool.mohaeng.admin.report.entity.AdminReportFEntity;
import org.poolpool.mohaeng.admin.report.repository.AdminReportRepository;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.notification.dto.NotificationItemDto;
import org.poolpool.mohaeng.notification.entity.NotificationEntity;
import org.poolpool.mohaeng.notification.entity.NotificationTypeEntity;
import org.poolpool.mohaeng.notification.repository.NotificationRepository;
import org.poolpool.mohaeng.notification.repository.NotificationTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final EventRepository eventRepository;
    private final AdminReportRepository reportRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static final String DEFAULT_STATUS = "미발송";
    private static final int STATUS_MAX_LEN = 50;

    private void publishChanged(long userId) {
        eventPublisher.publishEvent(new NotificationChangedEvent(userId));
    }

    private String normalizeStatus1(String value) {
        String v = (value == null || value.isBlank()) ? DEFAULT_STATUS : value.trim();
        return v.length() > STATUS_MAX_LEN ? v.substring(0, STATUS_MAX_LEN) : v;
    }

    private String normalizeStatus2(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        return v.length() > STATUS_MAX_LEN ? v.substring(0, STATUS_MAX_LEN) : v;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationItemDto> getList(long userId, Pageable pageable) {
        Page<NotificationEntity> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        if (page.isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
        }

        List<Long> typeIds = page.getContent().stream()
                .map(NotificationEntity::getNotiTypeId)
                .distinct()
                .toList();

        Map<Long, NotificationTypeEntity> typeMap = notificationTypeRepository.findAllByNotiTypeIdIn(typeIds).stream()
                .collect(Collectors.toMap(NotificationTypeEntity::getNotiTypeId, Function.identity()));

        List<Long> eventIds = page.getContent().stream()
                .map(NotificationEntity::getEventId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> eventTitleMap = eventRepository.findAllById(eventIds).stream()
                .collect(Collectors.toMap(EventEntity::getEventId, EventEntity::getTitle));

        List<Long> reportIds = page.getContent().stream()
                .map(NotificationEntity::getReportId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> reportReasonMap = reportRepository.findAllById(reportIds).stream()
                .collect(Collectors.toMap(AdminReportFEntity::getReportId, AdminReportFEntity::getReasonCategory));

        List<NotificationItemDto> items = page.getContent().stream()
                .map(n -> {
                    NotificationTypeEntity type = typeMap.get(n.getNotiTypeId());
                    String title = n.getEventId() == null ? "" : eventTitleMap.getOrDefault(n.getEventId(), "");
                    String reasonCategory = n.getReportId() == null ? "" : reportReasonMap.getOrDefault(n.getReportId(), "");
                    String contents = applyTemplate(type, title, reasonCategory);
                    return NotificationItemDto.fromEntity(n, type, contents);
                })
                .toList();

        return new PageResponse<>(
                items,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long count(long userId) {
        return notificationRepository.countByUserId(userId);
    }

    @Override
    @Transactional
    public void read(long userId, long notificationId) {
        int affected = notificationRepository.deleteByNotificationIdAndUserId(notificationId, userId);
        if (affected == 0) {
            throw new IllegalArgumentException("알림이 없거나 본인 알림이 아닙니다.");
        }
        publishChanged(userId);
    }

    @Override
    @Transactional
    public void readAll(long userId) {
        int deleted = notificationRepository.deleteByUserId(userId);
        if (deleted > 0) {
            publishChanged(userId);
        }
    }

    @Override
    @Transactional
    public long create(long userId, long notiTypeId, Long eventId, Long reportId) {
        if (!notificationTypeRepository.existsById(notiTypeId)) {
            throw new EntityNotFoundException("존재하지 않는 알림 타입입니다.");
        }

        NotificationEntity n = NotificationEntity.builder()
                .userId(userId)
                .notiTypeId(notiTypeId)
                .eventId(eventId)
                .reportId(reportId)
                .status1(DEFAULT_STATUS)
                .status2(null)
                .build();

        long id = notificationRepository.save(n).getNotificationId();
        log.info("Notification created id={} userId={} typeId={} eventId={} reportId={}",
                id, userId, notiTypeId, eventId, reportId);

        publishChanged(userId);
        return id;
    }

    @Override
    @Transactional
    public long createWithStatus(long userId, long notiTypeId, Long eventId, Long reportId, String status1, String status2) {
        if (!notificationTypeRepository.existsById(notiTypeId)) {
            throw new EntityNotFoundException("존재하지 않는 알림 타입입니다.");
        }

        NotificationEntity n = NotificationEntity.builder()
                .userId(userId)
                .notiTypeId(notiTypeId)
                .eventId(eventId)
                .reportId(reportId)
                .status1(normalizeStatus1(status1))
                .status2(normalizeStatus2(status2))
                .build();

        long id = notificationRepository.save(n).getNotificationId();
        log.info("Notification createWithStatus id={} userId={} typeId={} eventId={} reportId={} status1={} status2={}",
                id, userId, notiTypeId, eventId, reportId, n.getStatus1(), n.getStatus2());

        publishChanged(userId);
        return id;
    }

    private String reasonCategoryLabel(String code) {
        if (code == null) return "";
        String v = code.trim();
        if (v.isEmpty()) return "";

        return switch (v) {
            case "SPAM" -> "광고/스팸/도배";
            case "FRAUD" -> "허위 정보/내용 불일치";
            case "COPYRIGHT" -> "도용/사칭/저작권 침해";
            case "INAPPROPRIATE", "ABUSE", "ADULT", "ILLEGAL" -> "부적절한 내용";
            case "DUPLICATE" -> "중복/반복 등록";
            case "OTHER" -> "기타";
            default -> v;
        };
    }

    private String applyTemplate(NotificationTypeEntity type, String title, String reasonCategory) {
        if (type == null || type.getNotiTypeContents() == null) return "";

        String rc = reasonCategoryLabel(reasonCategory);

        return type.getNotiTypeContents()
                .replace("[TITLE]", title == null ? "" : title)
                .replace("[REASON_CATEGORY]", rc);
    }
}