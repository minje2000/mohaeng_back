package org.poolpool.mohaeng.ai.dto;

import lombok.Data;

@Data
public class BizOcrResponse {
    private String businessNumber;
    private String companyName;
    private String ownerName;
    private String openDate;
    private String taxType;
    private String validationStatus;
    private String validationMessage;
}