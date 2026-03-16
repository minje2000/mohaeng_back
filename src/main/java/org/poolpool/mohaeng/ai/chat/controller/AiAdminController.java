package org.poolpool.mohaeng.ai.chat.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.poolpool.mohaeng.ai.chat.service.AiChatProxyService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/ai-legacy")
@RequiredArgsConstructor
public class AiAdminController {

    private final AiChatProxyService aiChatProxyService;

    @GetMapping("/contacts")
    public Map<String, Object> contacts(
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        List<Map<String, Object>> items = aiChatProxyService.getAdminContacts(limit, authorizationHeader);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        return response;
    }

    @GetMapping("/logs")
    public Map<String, Object> logs(
            @RequestParam(name = "limit", defaultValue = "150") int limit,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        List<Map<String, Object>> items = aiChatProxyService.getAdminLogs(limit, authorizationHeader);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        return response;
    }
}
