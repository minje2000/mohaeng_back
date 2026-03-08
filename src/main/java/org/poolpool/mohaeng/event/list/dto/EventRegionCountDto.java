package org.poolpool.mohaeng.event.list.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EventRegionCountDto {
    private Long regionId;
    private Long count;

    // JPQL의 'new' 연산자가 이 생성자를 사용해 데이터를 주입합니다.
    public EventRegionCountDto(Long regionId, Long count) {
        this.regionId = regionId;
        this.count = count;
    }
}