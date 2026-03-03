package org.poolpool.mohaeng.event.mypage.service;

import java.util.List;

import org.poolpool.mohaeng.auth.exception.AuthException;
import org.poolpool.mohaeng.event.mypage.dto.BoothApplicationDetailResponse;
import org.poolpool.mohaeng.event.mypage.dto.BoothMypageResponse;
import org.poolpool.mohaeng.event.mypage.repository.MypageBoothRepository;
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.type.NotiTypeId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MypageBoothService {

    private final MypageBoothRepository repo;
    private final NotificationService notificationService;

    public List<BoothMypageResponse> getMyBooths(Long userId) {
        return repo.findMyBooths(userId);
    }

    public List<BoothMypageResponse> getReceivedBooths(Long hostUserId) {
        return repo.findReceivedBooths(hostUserId);
    }

    public BoothApplicationDetailResponse getBoothApplicationDetail(Long viewerId, Long pctBoothId) {
        BoothApplicationDetailResponse res = repo.findBoothDetail(viewerId, pctBoothId);
        if (res == null) {
            throw AuthException.forbidden("BOOTH_FORBIDDEN", "조회 권한이 없거나 신청 내역을 찾을 수 없습니다.");
        }
        return res;
    }

    @Transactional
    public void approveBooth(Long hostUserId, Long pctBoothId) {
        int updated = repo.updateBoothStatusAsHostRelaxed(hostUserId, pctBoothId, "승인");
        if (updated == 0) {
            throw AuthException.badRequest("BOOTH_CANNOT_APPROVE", "승인할 수 없는 상태이거나 처리 권한이 없습니다.");
        }

        BoothApplicationDetailResponse detail = repo.findBoothDetail(hostUserId, pctBoothId);
        if (detail != null) {
            Long applicantId = detail.getUserId();
            Long eventId = detail.getEventId();

            if (applicantId != null && eventId != null && !applicantId.equals(hostUserId)) {
                notificationService.createWithStatus(
                        applicantId,
                        NotiTypeId.BOOTH_ACCEPT,          // 9
                        eventId,
                        null,
                        "미발송",
                        String.valueOf(pctBoothId)        //  status2
                );
            }
        }
    }

    @Transactional
    public void rejectBooth(Long hostUserId, Long pctBoothId) {
        int updated = repo.updateBoothStatusAsHostRelaxed(hostUserId, pctBoothId, "반려");
        if (updated == 0) {
            throw AuthException.badRequest("BOOTH_CANNOT_REJECT", "반려할 수 없는 상태이거나 처리 권한이 없습니다.");
        }

        BoothApplicationDetailResponse detail = repo.findBoothDetail(hostUserId, pctBoothId);
        if (detail != null) {
            Long applicantId = detail.getUserId();
            Long eventId = detail.getEventId();

            if (applicantId != null && eventId != null && !applicantId.equals(hostUserId)) {
                notificationService.createWithStatus(
                        applicantId,
                        NotiTypeId.BOOTH_REJECT,          // 10
                        eventId,
                        null,
                        "미발송",
                        String.valueOf(pctBoothId)        //  status2
                );
            }
        }

        // TODO: 환불 연동
    }
}