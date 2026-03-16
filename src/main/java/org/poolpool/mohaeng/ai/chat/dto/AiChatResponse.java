package org.poolpool.mohaeng.ai.chat.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatResponse {
    private String answer;
    private String intent;
    private String sessionId;
    private List<AiChatCardDto> cards = new ArrayList<>();
    private List<AiChatSourceDto> sources = new ArrayList<>();
}
