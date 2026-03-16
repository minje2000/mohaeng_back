package org.poolpool.mohaeng.ai.chat.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiChatRequest {
    private String message;
    private String sessionId;
    private String pageType;
    private String contextPage;
    private String region;
    private List<String> locationKeywords;
    private List<Map<String, Object>> history;
    private Map<String, Object> filters;
}
