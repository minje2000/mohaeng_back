package org.poolpool.mohaeng.ai.chat.contact.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContactResponseDto {
    private String id;
    private String ticketId;
    private String status;
    private String content;
    private String answer;
    private String sessionId;
    private String source;
    private String assignee;
    private String category;
    private String priority;
    private String createdAt;
    private String updatedAt;
    private String answeredAt;
    private String userId;
    private Boolean hasAuthorization;
    @Builder.Default
    private List<Map<String, Object>> history = new ArrayList<>();
}
