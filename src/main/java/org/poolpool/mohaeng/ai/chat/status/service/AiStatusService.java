package org.poolpool.mohaeng.ai.chat.status.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.poolpool.mohaeng.ai.chat.status.dto.AiStatusItemDto;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.event.mypage.dto.BoothMypageResponse;
import org.poolpool.mohaeng.event.mypage.service.MypageBoothService;
import org.poolpool.mohaeng.payment.entity.PaymentEntity;
import org.poolpool.mohaeng.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiStatusService {

    private final PaymentRepository paymentRepository;
    private final EventRepository eventRepository;
    private final MypageBoothService mypageBoothService;

    public List<AiStatusItemDto> getPaymentStatuses(Long userId) {
        List<PaymentEntity> payments = paymentRepository.findTop10ByUserIdOrderByPaymentIdDesc(userId);
        Map<Long, EventEntity> eventMap = loadEvents(payments.stream().map(PaymentEntity::getEventId).collect(Collectors.toSet()));
        return payments.stream().map(payment -> toPaymentDto(payment, eventMap.get(payment.getEventId()))).toList();
    }

    public List<AiStatusItemDto> getRefundStatuses(Long userId) {
        List<PaymentEntity> payments = paymentRepository.findTop10ByUserIdAndPaymentStatusInOrderByPaymentIdDesc(
                userId,
                List.of("PARTIAL_CANCEL", "CANCELLED", "CANCELED")
        );
        Map<Long, EventEntity> eventMap = loadEvents(payments.stream().map(PaymentEntity::getEventId).collect(Collectors.toSet()));
        return payments.stream().map(payment -> toRefundDto(payment, eventMap.get(payment.getEventId()))).toList();
    }

    public List<AiStatusItemDto> getBoothStatuses(Long userId) {
        List<BoothMypageResponse> booths = mypageBoothService.getMyBooths(userId);
        return booths.stream().limit(10).map(this::toBoothDto).toList();
    }

    private Map<Long, EventEntity> loadEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        return eventRepository.findAllById(eventIds).stream().collect(Collectors.toMap(EventEntity::getEventId, item -> item));
    }

    private AiStatusItemDto toPaymentDto(PaymentEntity payment, EventEntity event) {
        return AiStatusItemDto.builder()
                .eventId(payment.getEventId())
                .eventTitle(event != null ? event.getTitle() : "행사")
                .description(event != null ? firstText(event.getSimpleExplain(), event.getDescription()) : null)
                .thumbnail(event != null ? event.getThumbnail() : null)
                .startDate(event != null && event.getStartDate() != null ? event.getStartDate().toString() : null)
                .endDate(event != null && event.getEndDate() != null ? event.getEndDate().toString() : null)
                .payType(payment.getPayType())
                .paymentStatus(payment.getPaymentStatus())
                .amountTotal(payment.getAmountTotal())
                .canceledAmount(payment.getCanceledAmount())
                .pctId(payment.getPctId())
                .pctBoothId(payment.getPctBoothId())
                .build();
    }

    private AiStatusItemDto toRefundDto(PaymentEntity payment, EventEntity event) {
        String refundStatus = payment.getPaymentStatus();
        if ("PARTIAL_CANCEL".equalsIgnoreCase(refundStatus)) {
            refundStatus = "부분 환불 완료";
        } else if ("CANCELLED".equalsIgnoreCase(refundStatus) || "CANCELED".equalsIgnoreCase(refundStatus)) {
            refundStatus = "전액 환불 완료";
        }
        return AiStatusItemDto.builder()
                .eventId(payment.getEventId())
                .eventTitle(event != null ? event.getTitle() : "행사")
                .description(event != null ? firstText(event.getSimpleExplain(), event.getDescription()) : null)
                .thumbnail(event != null ? event.getThumbnail() : null)
                .startDate(event != null && event.getStartDate() != null ? event.getStartDate().toString() : null)
                .endDate(event != null && event.getEndDate() != null ? event.getEndDate().toString() : null)
                .payType(payment.getPayType())
                .paymentStatus(payment.getPaymentStatus())
                .refundStatus(refundStatus)
                .amountTotal(payment.getAmountTotal())
                .canceledAmount(payment.getCanceledAmount())
                .pctId(payment.getPctId())
                .pctBoothId(payment.getPctBoothId())
                .build();
    }

    private AiStatusItemDto toBoothDto(BoothMypageResponse booth) {
        return AiStatusItemDto.builder()
                .eventId(booth.getEventId())
                .eventTitle(firstText(booth.getEventTitle(), booth.getBoothTitle(), "부스 신청"))
                .description(firstText(booth.getEventDescription(), booth.getBoothTopic()))
                .thumbnail(booth.getEventThumbnail())
                .startDate(booth.getStartDate() != null ? booth.getStartDate().toString() : null)
                .endDate(booth.getEndDate() != null ? booth.getEndDate().toString() : null)
                .pctBoothId(booth.getPctBoothId())
                .status(booth.getStatus())
                .totalPrice(booth.getTotalPrice())
                .build();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
