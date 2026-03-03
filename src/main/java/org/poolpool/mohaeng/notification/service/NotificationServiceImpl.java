package org.poolpool.mohaeng.notification.service;

import java.util.HashMap;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;

    private final EventRepository eventRepository;
    private final AdminReportRepository reportRepository;

    @PersistenceContext
    private EntityManager em;

    private final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static boolean isBoothNoti(Long notiTypeId) {
        return notiTypeId != null && (notiTypeId == 8L || notiTypeId == 9L || notiTypeId == 10L);
    }

    private static Long parseLongOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || "0".equals(t)) return null;

        String digits = t.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;

        try { return Long.valueOf(digits); } catch (Exception e) { return null; }
    }

    //  부스 키는 status2 우선
    private static Long boothIdFrom(NotificationEntity n) {
        Long v = parseLongOrNull(n.getStatus2());
        if (v != null) return v;
        return parseLongOrNull(n.getStatus1());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationItemDto> getList(long userId, Pageable pageable) {
        Page<NotificationEntity> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        if (page.isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
        }

        // 1) 타입
        List<Long> typeIds = page.getContent().stream()
                .map(NotificationEntity::getNotiTypeId)
                .distinct()
                .toList();

        Map<Long, NotificationTypeEntity> typeMap = notificationTypeRepository.findAllByNotiTypeIdIn(typeIds).stream()
                .collect(Collectors.toMap(NotificationTypeEntity::getNotiTypeId, Function.identity()));

        // 2) 행사 제목
        List<Long> eventIds = page.getContent().stream()
                .map(NotificationEntity::getEventId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> eventTitleMap = eventRepository.findAllById(eventIds).stream()
                .collect(Collectors.toMap(EventEntity::getEventId, EventEntity::getTitle));

        // 3) 신고 사유
        List<Long> reportIds = page.getContent().stream()
                .map(NotificationEntity::getReportId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> reportReasonMap = reportRepository.findAllById(reportIds).stream()
                .collect(Collectors.toMap(AdminReportFEntity::getReportId, AdminReportFEntity::getReasonCategory));

        //  4) 부스 제목 맵 (pctBoothId -> boothTitle)
        List<Long> boothIds = page.getContent().stream()
                .filter(n -> isBoothNoti(n.getNotiTypeId()))
                .map(NotificationServiceImpl::boothIdFrom)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> boothTitleMap = new HashMap<>();
        if (!boothIds.isEmpty()) {
            // ParticipationBoothEntity의 필드명이 pctBoothId, boothTitle 이어야 함
            List<Object[]> rows = em.createQuery(
                    "select b.pctBoothId, b.boothTitle " +
                    "from ParticipationBoothEntity b " +
                    "where b.pctBoothId in :ids",
                    Object[].class
            ).setParameter("ids", boothIds).getResultList();

            for (Object[] r : rows) {
                boothTitleMap.put((Long) r[0], (String) r[1]);
            }
        }

        // 5) DTO 변환
        List<NotificationItemDto> items = page.getContent().stream()
                .map(n -> {
                    NotificationTypeEntity type = typeMap.get(n.getNotiTypeId());

                    String title = (n.getEventId() == null) ? "" : eventTitleMap.getOrDefault(n.getEventId(), "");

                    if (isBoothNoti(n.getNotiTypeId())) {
                        Long boothId = boothIdFrom(n);
                        String boothTitle = (boothId == null) ? null : boothTitleMap.get(boothId);
                        if (boothTitle != null && !boothTitle.isBlank()) {
                            title = title.isBlank() ? boothTitle : (title + " / " + boothTitle);
                        }
                    }

                    String reasonCategory = (n.getReportId() == null)
                            ? ""
                            : reportReasonMap.getOrDefault(n.getReportId(), "");

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
    }

    @Override
    @Transactional
    public void readAll(long userId) {
        int affected = notificationRepository.deleteByUserId(userId);
        if (affected == 0) {
            throw new IllegalArgumentException("삭제할 알림이 없습니다.");
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
                .status1("미발송")
                .status2("0")
                .build();

        NotificationEntity saved = notificationRepository.save(n);
        log.info("Notification created. id={}, userId={}, typeId={}", saved.getNotificationId(), userId, notiTypeId);
        return saved.getNotificationId();
    }

    @Override
    @Transactional
    public long createWithStatus(long userId, long notiTypeId, Long eventId, Long reportId, String status1, String status2) {
        if (!notificationTypeRepository.existsById(notiTypeId)) {
            throw new EntityNotFoundException("존재하지 않는 알림 타입입니다.");
        }

        String s1 = (status1 == null || status1.isBlank()) ? "미발송" : status1;
        String s2 = (status2 == null || status2.isBlank()) ? "0" : status2;

        NotificationEntity n = NotificationEntity.builder()
                .userId(userId)
                .notiTypeId(notiTypeId)
                .eventId(eventId)
                .reportId(reportId)
                .status1(s1)
                .status2(s2)
                .build();

        return notificationRepository.save(n).getNotificationId();
    }

    private String applyTemplate(NotificationTypeEntity type, String title, String reasonCategory) {
        if (type == null || type.getNotiTypeContents() == null) return "";
        return type.getNotiTypeContents()
                .replace("[TITLE]", title == null ? "" : title)
                .replace("[REASON_CATEGORY]", reasonCategory == null ? "" : reasonCategory);
    }
}