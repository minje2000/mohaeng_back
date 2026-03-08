package org.poolpool.mohaeng.event.list.dto;

import org.poolpool.mohaeng.event.list.entity.EventRegionEntity;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventRegionDto {
    private Long regionId;
    private String regionName;
    private String parentName;

    // Entity -> DTO
    public static EventRegionDto fromEntity(EventRegionEntity entity) {
        if (entity == null) return null;
        return EventRegionDto.builder()
                .regionId(entity.getRegionId())
                .regionName(entity.getRegionName())
                .parentName(entity.getParent() != null ? entity.getParent().getRegionName() : null)
                .build();
    }

    // 💡 [핵심] 이 메서드가 없어서 EventDto에서 에러가 났던 겁니다!
    public EventRegionEntity toEntity() {
        return EventRegionEntity.builder()
                .regionId(this.regionId)
                .regionName(this.regionName)
                // 부모 객체는 여기서 굳이 만들지 않아도 됩니다.
                .build();
    }
}