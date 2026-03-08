package org.poolpool.mohaeng.event.host.dto;

import lombok.*;
import org.poolpool.mohaeng.event.host.entity.HostFacilityEntity;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostFacilityDto {
    
    private Long hostBoothfaciId;
    private Long eventId;
    private String faciName;
    private Integer faciPrice;
    private String faciUnit;
    private Boolean hasCount;
    private Integer totalCount;
    private Integer remainCount;
    private LocalDateTime createdAt;

    // Entity -> DTO 변환
    public static HostFacilityDto fromEntity(HostFacilityEntity entity) {
        if (entity == null) return null;
        return HostFacilityDto.builder()
                .hostBoothfaciId(entity.getHostBoothfaciId())
                .eventId(entity.getEventId())
                .faciName(entity.getFaciName())
                .faciPrice(entity.getFaciPrice())
                .faciUnit(entity.getFaciUnit())
                .hasCount(entity.getHasCount())
                .totalCount(entity.getTotalCount())
                .remainCount(entity.getRemainCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // DTO -> Entity 변환
    public HostFacilityEntity toEntity() {
        return HostFacilityEntity.builder()
                .hostBoothfaciId(this.hostBoothfaciId)
                .eventId(this.eventId)
                .faciName(this.faciName)
                .faciPrice(this.faciPrice != null ? this.faciPrice : 0)
                .faciUnit(this.faciUnit)
                .hasCount(this.hasCount != null ? this.hasCount : false)
                .totalCount(this.totalCount != null ? this.totalCount : 0)
                .remainCount(this.remainCount)
                .build();
    }
}
