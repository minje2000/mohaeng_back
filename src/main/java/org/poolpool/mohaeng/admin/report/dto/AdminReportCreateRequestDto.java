package org.poolpool.mohaeng.admin.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReportCreateRequestDto {

    @NotNull
    private Long eventId;

    @NotBlank
    @Size(max = 50)
    private String reasonCategory;

    @Size(max = 2000)
    private String reasonDetailText;
}