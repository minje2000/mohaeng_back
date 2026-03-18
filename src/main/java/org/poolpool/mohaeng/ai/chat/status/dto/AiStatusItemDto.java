package org.poolpool.mohaeng.ai.chat.status.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiStatusItemDto {
    private Long eventId;
    private String eventTitle;
    private String description;
    private String thumbnail;
    private String startDate;
    private String endDate;
    private String payType;
    private String paymentStatus;
    private Integer amountTotal;
    private Integer canceledAmount;
    private String refundStatus;
    private Long pctId;
    private Long pctBoothId;
    private String status;
    private Integer totalPrice;
}
