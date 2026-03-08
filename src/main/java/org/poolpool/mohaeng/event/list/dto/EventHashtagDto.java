package org.poolpool.mohaeng.event.list.dto;

import org.poolpool.mohaeng.event.list.entity.EventHashtagEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventHashtagDto {
    private Integer hashtagId;
    private String hashtagName;

    // Entity -> DTO 변환
    public static EventHashtagDto fromEntity(EventHashtagEntity entity) {
        if (entity == null) return null;
        
        return EventHashtagDto.builder()
                .hashtagId(entity.getHashtagId())
                .hashtagName(entity.getHashtagName())
                .build();
    }

    // DTO -> Entity 변환 
    public EventHashtagEntity toEntity() {
        return EventHashtagEntity.builder()
                .hashtagId(this.hashtagId)
                .hashtagName(this.hashtagName)
                .build();
    }
}