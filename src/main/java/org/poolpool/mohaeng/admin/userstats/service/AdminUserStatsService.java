package org.poolpool.mohaeng.admin.userstats.service;

import java.util.List;
import org.poolpool.mohaeng.admin.userstats.dto.UserStatsDto;

public interface AdminUserStatsService {
    UserStatsDto getDashboardStats();
    List<UserStatsDto> findMonthlyUsers();
    List<UserStatsDto> getDormantHandle();
    List<UserStatsDto> getDormantHandleByMonth(int year, String month); // ✅ 추가
}
