package org.poolpool.mohaeng.admin.userstats.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.poolpool.mohaeng.admin.userstats.dto.UserStatsDto;
import org.poolpool.mohaeng.admin.userstats.repository.AdminUserStatsRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserStatsServiceImpl implements AdminUserStatsService {

    private final AdminUserStatsRepository adminUserStatsRepository;

    @Override
    public UserStatsDto getDashboardStats() {
        return adminUserStatsRepository.findUserDashboardStats(LocalDate.now());
    }

    @Override
    public List<UserStatsDto> findMonthlyUsers() {
        LocalDate now = LocalDate.now();
        LocalDateTime sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1).atStartOfDay();
        List<UserStatsDto> monthlyUsers = adminUserStatsRepository.findMonthlyUsers(sixMonthsAgo);

        Map<String, Long> resultMap = monthlyUsers.stream()
                .collect(Collectors.toMap(UserStatsDto::getPeriod, UserStatsDto::getUserCount));

        List<UserStatsDto> acc = new ArrayList<>();
        long cumulative = 0;
        for (int i = 5; i >= 0; i--) {
            String period = now.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            cumulative += resultMap.getOrDefault(period, 0L);
            acc.add(new UserStatsDto(period, cumulative));
        }
        return acc;
    }

    @Override
    public List<UserStatsDto> getDormantHandle() {
        LocalDate now = LocalDate.now();
        LocalDateTime sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1).atStartOfDay();

        Map<String, Long> notifiedMap = adminUserStatsRepository.findMonthlyDormantNotified(sixMonthsAgo)
                .stream().collect(Collectors.toMap(UserStatsDto::getPeriod, UserStatsDto::getDormantNotifiedCount));
        Map<String, Long> withdrawnMap = adminUserStatsRepository.findMonthlyDormantWithdrawn(sixMonthsAgo)
                .stream().collect(Collectors.toMap(UserStatsDto::getPeriod, UserStatsDto::getDormantWithdrawnCount));

        List<UserStatsDto> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String period = now.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            result.add(new UserStatsDto(period,
                    notifiedMap.getOrDefault(period, 0L),
                    withdrawnMap.getOrDefault(period, 0L)));
        }
        return result;
    }

    // ✅ 선택한 년/월의 일별 조치 동향 - 두 쿼리 결과를 날짜 기준으로 합산
    @Override
    public List<UserStatsDto> getDormantHandleByMonth(int year, String month) {
        String yearMonth = year + "-" + month; // "2026-02"

        Map<String, Long> notifiedMap = adminUserStatsRepository.findDailyDormantNotified(yearMonth)
                .stream().collect(Collectors.toMap(UserStatsDto::getPeriod, UserStatsDto::getDormantNotifiedCount));
        Map<String, Long> withdrawnMap = adminUserStatsRepository.findDailyDormantWithdrawn(yearMonth)
                .stream().collect(Collectors.toMap(UserStatsDto::getPeriod, UserStatsDto::getDormantWithdrawnCount));

        // 두 Map의 날짜 키를 합쳐서 정렬
        TreeMap<String, UserStatsDto> merged = new TreeMap<>();
        notifiedMap.forEach((day, cnt) ->
            merged.computeIfAbsent(day, d -> new UserStatsDto(d, 0L, 0L))
                  .setDormantNotifiedCount(cnt));
        withdrawnMap.forEach((day, cnt) ->
            merged.computeIfAbsent(day, d -> new UserStatsDto(d, 0L, 0L))
                  .setDormantWithdrawnCount(cnt));

        return new ArrayList<>(merged.values());
    }
}
