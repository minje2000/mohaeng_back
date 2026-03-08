package org.poolpool.mohaeng.event.host.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.poolpool.mohaeng.event.list.dto.EventDto;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventCreateDto {
    // 1. 행사 기본 정보 (동결 DTO)
    private EventDto eventInfo; 
    
    // 2. 부스 목록
    private List<HostBoothDto> booths; 
    
    // 3. 부대시설 목록
    private List<HostFacilityDto> facilities; 
}