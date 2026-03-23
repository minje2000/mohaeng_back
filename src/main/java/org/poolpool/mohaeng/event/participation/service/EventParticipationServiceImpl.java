package org.poolpool.mohaeng.event.participation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.poolpool.mohaeng.auth.security.principal.CustomUserPrincipal;
import org.poolpool.mohaeng.event.host.entity.HostBoothEntity;
import org.poolpool.mohaeng.event.host.repository.HostBoothRepository;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.event.participation.dto.EventParticipationDto;
import org.poolpool.mohaeng.event.participation.entity.EventParticipationEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothFacilityEntity;
import org.poolpool.mohaeng.event.participation.repository.EventParticipationRepository;
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.type.NotiTypeId;
import org.poolpool.mohaeng.payment.entity.PaymentEntity;
import org.poolpool.mohaeng.payment.repository.PaymentRepository;
import org.poolpool.mohaeng.payment.service.PaymentService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventParticipationServiceImpl implements EventParticipationService {

    private final EventParticipationRepository participationRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final EventRepository eventRepository;
    private final HostBoothRepository hostBoothRepository;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof CustomUserPrincipal cup) {
            return Long.parseLong(cup.getUserId());
        }
        if (principal instanceof UserDetails ud) {
            return Long.parseLong(ud.getUsername());
        }
        if (principal instanceof String s) {
            return Long.parseLong(s);
        }

        throw new IllegalStateException("인증 정보를 찾을 수 없습니다. principal=" + principal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventParticipationDto> getParticipationList(Long userId) {
        List<EventParticipationEntity> list = participationRepository.findParticipationsByUserId(userId);

        return list.stream()
                .filter(p -> !"참여삭제".equals(p.getPctStatus()))
                .map(p -> {
                    EventEntity event = null;
                    try {
                        event = eventRepository.findById(p.getEventId()).orElse(null);
                    } catch (Exception ignored) {}

                    Integer payAmount = null;
                    try {
                        payAmount = paymentRepository.findByPctId(p.getPctId())
                                .map(PaymentEntity::getAmountTotal)
                                .orElse(null);
                    } catch (Exception ignored) {}

                    return EventParticipationDto.fromEntityWithEvent(p, event, payAmount);
                })
                .toList();
    }

    @Override
    public Long submitParticipation(Long userId, Long eventId) {
        if (participationRepository.existsActiveParticipation(userId, eventId)) {
            throw new IllegalStateException("이미 신청한 행사입니다.");
        }

        EventEntity event = findEvent(eventId);
        boolean isPaid = event.getPrice() != null && event.getPrice() > 0;
        String initialStatus = isPaid ? "결제대기" : "참여확정";

        EventParticipationEntity pct = new EventParticipationEntity();
        pct.setEventId(eventId);
        pct.setUserId(userId);
        pct.setPctStatus(initialStatus);

        participationRepository.saveParticipation(pct);
        log.info("[행사 참여 신청] userId={}, eventId={}, status={}", userId, eventId, initialStatus);
        return pct.getPctId();
    }

    @Override
    public void cancelParticipation(Long pctId) {
        EventParticipationEntity pct = participationRepository.findParticipationById(pctId)
                .orElseThrow(() -> new IllegalArgumentException("참여 신청 정보를 찾을 수 없습니다."));

        if (!Set.of("임시저장", "신청", "결제대기", "결제완료", "참여확정").contains(pct.getPctStatus())) {
            throw new IllegalStateException("현재 상태(" + pct.getPctStatus() + ")에서는 취소할 수 없습니다.");
        }

        EventEntity event = findEvent(pct.getEventId());
        int refundRate = calcRefundRate(event.getStartDate());

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByPctId(pctId);
        refundIfPaid(paymentOpt, refundRate, "참가자 취소");

        pct.setPctStatus("취소");
        participationRepository.saveParticipation(pct);
        log.info("[행사 참여 취소] pctId={}, refundRate={}%, paymentExists={}", pctId, refundRate, paymentOpt.isPresent());
    }

    @Override
    public Long submitBoothParticipation(Long userId, Long eventId, Object dto) {
        throw new UnsupportedOperationException(
                "부스 신청 제출은 /api/eventParticipation/submitBoothApply 컨트롤러에서 처리합니다.");
    }

    @Override
    public void cancelBoothParticipation(Long pctBoothId) {
        ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId)
                .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다."));

        if (Set.of("승인", "반려").contains(booth.getStatus())) {
            throw new IllegalStateException("승인 또는 반려된 부스는 취소할 수 없습니다.");
        }

        Long eventId = getEventIdFromHostBooth(booth.getHostBoothId());
        int refundRate = calcRefundRate(findEvent(eventId).getStartDate());

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByPctBoothId(pctBoothId);
        refundIfPaid(paymentOpt, refundRate, "참가자 부스 취소");

        restoreInventory(pctBoothId, booth.getHostBoothId(), booth.getBoothCount());

        booth.setStatus("취소");
        participationRepository.saveBooth(booth);
        log.info("[부스 취소] pctBoothId={}, refundRate={}%, paymentExists={}", pctBoothId, refundRate, paymentOpt.isPresent());
    }

    @Override
    public void approveBooth(Long pctBoothId) {
        ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId)
                .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다."));

        if (!Set.of("신청", "결제완료", "결제대기").contains(booth.getStatus())) {
            throw new IllegalStateException("신청/결제 상태에서만 승인 가능합니다.");
        }

        Long eventId = getEventIdFromHostBooth(booth.getHostBoothId());

        booth.setStatus("승인");
        booth.setApprovedDate(LocalDateTime.now());
        participationRepository.saveBooth(booth);

        notificationService.create(
                booth.getUserId(),
                NotiTypeId.BOOTH_ACCEPT,
                eventId,
                null
        );

        log.info("[부스 승인] pctBoothId={}, eventId={}, userId={}", pctBoothId, eventId, booth.getUserId());
    }

    @Override
    public void rejectBooth(Long pctBoothId) {
        ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId)
                .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다."));

        if (!Set.of("신청", "결제완료", "결제대기").contains(booth.getStatus())) {
            throw new IllegalStateException("신청/결제 상태에서만 반려 가능합니다.");
        }

        Long eventId = getEventIdFromHostBooth(booth.getHostBoothId());

        Optional<PaymentEntity> paymentOpt = paymentRepository.findByPctBoothId(pctBoothId);
        refundIfPaid(paymentOpt, 100, "주최자 반려");

        restoreInventory(pctBoothId, booth.getHostBoothId(), booth.getBoothCount());

        booth.setStatus("반려");
        participationRepository.saveBooth(booth);

        notificationService.create(
                booth.getUserId(),
                NotiTypeId.BOOTH_REJECT,
                eventId,
                null
        );

        log.info("[부스 반려] pctBoothId={}, eventId={}, userId={} → 100% 환불 + 재고 복원",
                pctBoothId, eventId, booth.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveParticipation(Long eventId) {
        Long userId = getCurrentUserId();
        return participationRepository.existsActiveParticipation(userId, eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveBooth(Long eventId) {
        Long userId = getCurrentUserId();
        return participationRepository.existsActiveBoothParticipation(userId, eventId);
    }

    private EventEntity findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("행사를 찾을 수 없습니다. eventId=" + eventId));
    }

    private Long getEventIdFromHostBooth(Long hostBoothId) {
        HostBoothEntity hb = hostBoothRepository.findById(hostBoothId)
                .orElseThrow(() -> new IllegalArgumentException("HostBooth를 찾을 수 없습니다. boothId=" + hostBoothId));
        return hb.getEventId();
    }

    private int calcRefundRate(LocalDate startDate) {
        if (startDate == null) return 0;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), startDate);
        if (days >= 30) return 100;
        if (days >= 15) return 80;
        if (days >= 7) return 50;
        if (days >= 3) return 30;
        return 0;
    }

    private void refundIfPaid(Optional<PaymentEntity> paymentOpt, int refundRate, String reason) {
        if (paymentOpt.isEmpty()) return;

        PaymentEntity p = paymentOpt.get();
        if (p.getPaymentKey() == null || p.getPaymentKey().isBlank()) return;
        if ("CANCELED".equalsIgnoreCase(p.getPaymentStatus())) return;
        if (!"APPROVED".equalsIgnoreCase(p.getPaymentStatus())) return;

        int total = p.getAmountTotal() == null ? 0 : p.getAmountTotal();
        int cancelAmount = (int) Math.floor(total * (refundRate / 100.0));
        if (cancelAmount <= 0) return;

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);

        try {
            template.executeWithoutResult(status -> {
                paymentService.cancelPayment(p.getPaymentKey(), cancelAmount, reason);
            });
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("이미 취소된 결제")) {
                log.warn("[환불 스킵] 이미 취소된 결제 paymentKey={}", p.getPaymentKey());
                p.setPaymentStatus("CANCELED");
                paymentRepository.save(p);
                return;
            }
            throw e;
        }
    }

    private void restoreInventory(Long pctBoothId, Long hostBoothId, Integer boothCount) {
        List<ParticipationBoothFacilityEntity> facilities = participationRepository.findFacilitiesByPctBoothId(pctBoothId);
        for (ParticipationBoothFacilityEntity f : facilities) {
            if (f.getHostBoothFaciId() == null) continue;
            int cnt = (f.getFaciCount() == null || f.getFaciCount() <= 0) ? 1 : f.getFaciCount();
            participationRepository.increaseFacilityRemainCount(f.getHostBoothFaciId(), cnt);
        }

        int bc = (boothCount == null || boothCount <= 0) ? 1 : boothCount;
        for (int i = 0; i < bc; i++) {
            participationRepository.increaseBoothRemainCount(hostBoothId);
        }
    }
}