package org.poolpool.mohaeng.admin.report.service;

import org.poolpool.mohaeng.admin.report.dto.AdminReportCreateRequestDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportDetailDto;
import org.poolpool.mohaeng.admin.report.dto.AdminReportListItemDto;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminReportService {
    PageResponse<AdminReportListItemDto> getList(Pageable pageable);
    AdminReportDetailDto getDetail(long reportId);
    long create(long reporterId, AdminReportCreateRequestDto request);
    void approve(long reportId);
    void reject(long reportId);
}