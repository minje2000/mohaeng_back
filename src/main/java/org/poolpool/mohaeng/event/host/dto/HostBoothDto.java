package org.poolpool.mohaeng.event.host.dto;

import lombok.*;
import org.poolpool.mohaeng.event.host.entity.HostBoothEntity;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostBoothDto {
    
    private Long boothId;
    private Long eventId;
    private String boothName;
    private Integer boothPrice;
    private String boothSize;
    private String boothNote;
    private Integer totalCount;
    private Integer remainCount;
    private LocalDateTime createdAt;

    // Entity -> DTO 변환
    public static HostBoothDto fromEntity(HostBoothEntity entity) {
        if (entity == null) return null;
        return HostBoothDto.builder()
                .boothId(entity.getBoothId())
                .eventId(entity.getEventId())
                .boothName(entity.getBoothName())
                .boothPrice(entity.getBoothPrice())
                .boothSize(entity.getBoothSize())
                .boothNote(entity.getBoothNote())
                .totalCount(entity.getTotalCount())
                .remainCount(entity.getRemainCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // DTO -> Entity 변환
    public HostBoothEntity toEntity() {
        return HostBoothEntity.builder()
                .boothId(this.boothId)
                .eventId(this.eventId)
                .boothName(this.boothName)
                .boothPrice(this.boothPrice != null ? this.boothPrice : 0)
                .boothSize(this.boothSize)
                .boothNote(this.boothNote)
                .totalCount(this.totalCount != null ? this.totalCount : 1)
                .remainCount(this.remainCount != null ? this.remainCount : 1)
                // createdAt은 DB에서 자동 생성되므로 보통 toEntity에서 제외합니다.
                .build();
    }
}