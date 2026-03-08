package org.poolpool.mohaeng.admin.userstats.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.poolpool.mohaeng.admin.userstats.dto.UserStatsDto;
import org.poolpool.mohaeng.admin.userstats.service.AdminUserStatsService;
import org.poolpool.mohaeng.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/userstats")
@RequiredArgsConstructor
public class AdminUserStatsController {

    private final AdminUserStatsService adminUserStatsService;

    // 운영 통계 조회 (대시보드 + 월별 회원 수 + 6개월 휴면 동향)
    @GetMapping("/getOperateStats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOperateStats() {
        UserStatsDto dashboardStats        = adminUserStatsService.getDashboardStats();
        List<UserStatsDto> monthlyUsers    = adminUserStatsService.findMonthlyUsers();
        List<UserStatsDto> monthlyDormant  = adminUserStatsService.getDormantHandle();

        Map<String, Object> map = new HashMap<>();
        map.put("dashboardStats",        dashboardStats);
        map.put("monthlyUsers",          monthlyUsers);
        map.put("monthlyDormantHandle",  monthlyDormant);

        return ResponseEntity.ok(ApiResponse.ok("운영 통계 조회 완료", map));
    }

    // ✅ 선택한 년/월의 일별 휴면계정 조치 동향 조회
    @GetMapping("/getDormantHandleByMonth")
    public ResponseEntity<ApiResponse<List<UserStatsDto>>> getDormantHandleByMonth(
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") String month) {
        List<UserStatsDto> result = adminUserStatsService.getDormantHandleByMonth(year, month);
        return ResponseEntity.ok(ApiResponse.ok("일별 휴면 조치 조회 완료", result));
    }
}
