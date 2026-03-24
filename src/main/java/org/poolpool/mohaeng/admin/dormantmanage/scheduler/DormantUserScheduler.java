package org.poolpool.mohaeng.admin.dormantmanage.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.poolpool.mohaeng.admin.dormantmanage.dto.DormantUserDto;
import org.poolpool.mohaeng.admin.dormantmanage.service.AdminDormantManageService;
import org.poolpool.mohaeng.admin.dormantmanage.type.DormantStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DormantUserScheduler {

    private final AdminDormantManageService adminDormantManageService;

    // 매일 새벽 3시 실행
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void handleDormantWithdrawalScheduler() {

        log.info("휴면 계정 탈퇴 스케줄러 시작");

        List<DormantUserDto> users =
                adminDormantManageService.findDormantUsersByDormantStatus(DormantStatus.NOTIFIED);

        LocalDateTime now = LocalDateTime.now();

        for (DormantUserDto user : users) {
        	
        	LocalDateTime lastLogin = user.getLastLoginAt().atStartOfDay();

            // 마지막 로그인 날짜와 현재 날짜가 7일 이상 차이나는 경우 탈퇴
            if (user.getWithdrawnAt() != null && now.isAfter(user.getWithdrawnAt()) && Duration.between(lastLogin, now).toDays() >= 7) {

                // 탈퇴 처리
                adminDormantManageService.handleWithdrawal(user);

                // 상태 변경
                adminDormantManageService.updateDormantUser(
                        DormantStatus.WITHDRAWN,
                        user
                );

                log.info("탈퇴 처리 완료 userId={}", user.getUserId());
            }
        }

        log.info("휴면 계정 탈퇴 스케줄러 종료");
    }
}
