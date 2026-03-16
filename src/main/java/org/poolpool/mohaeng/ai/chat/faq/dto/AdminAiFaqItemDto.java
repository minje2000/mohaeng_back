package org.poolpool.mohaeng.ai.chat.faq.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminAiFaqItemDto {
    private Long id;
    private String title;
    private String question;
    private String answer;
    private List<String> keywords = new ArrayList<>();
    private boolean enabled = true;
    private int sortOrder;
}
