package org.poolpool.mohaeng.event.list.dto;

import org.poolpool.mohaeng.event.list.entity.EventCategoryEntity;

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
public class EventCategoryDto {
    private Integer categoryId;
    private String categoryName;

    // Entity -> DTO 변환
    public static EventCategoryDto fromEntity(EventCategoryEntity entity) {
        if (entity == null) return null;
        
        return EventCategoryDto.builder()
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategoryName())
                .build();
    }

    // DTO -> Entity 변환 (이 부분이 추가되었습니다!)
    public EventCategoryEntity toEntity() {
        return EventCategoryEntity.builder()
                .categoryId(this.categoryId)
                .categoryName(this.categoryName)
                .build();
    }
}
