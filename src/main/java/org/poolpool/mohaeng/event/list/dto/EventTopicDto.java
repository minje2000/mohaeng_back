package org.poolpool.mohaeng.event.list.dto;

import org.poolpool.mohaeng.event.list.entity.EventTopicEntity;

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
public class EventTopicDto {
    private Integer topicId;
    private String topicName;

    // Entity -> DTO 변환
    public static EventTopicDto fromEntity(EventTopicEntity entity) {
        if (entity == null) return null;
        
        return EventTopicDto.builder()
                .topicId(entity.getTopicId())
                .topicName(entity.getTopicName())
                .build();
    }

    // DTO -> Entity 변환 
    public EventTopicEntity toEntity() {
        return EventTopicEntity.builder()
                .topicId(this.topicId)
                .topicName(this.topicName)
                .build();
    }
}
