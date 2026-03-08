package org.poolpool.mohaeng.event.list.dto;

import java.time.LocalDate;

public interface EventDailyCountDto {
    LocalDate getDate();
    Long getCount();
}