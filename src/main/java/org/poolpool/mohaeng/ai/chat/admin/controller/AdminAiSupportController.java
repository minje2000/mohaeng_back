package org.poolpool.mohaeng.ai.chat.admin.controller;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.ai.chat.admin.service.AiAdminProxyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiSupportController {

    private final AiAdminProxyService aiAdminProxyService;

    @GetMapping("/contacts")
    public Object getContacts() {
        return aiAdminProxyService.getContacts();
    }

    @PutMapping("/contacts/{itemId}")
    public Object answerContact(@PathVariable("itemId") Long itemId, @RequestBody Map<String, Object> payload) {
        return aiAdminProxyService.answerContact(itemId, payload);
    }

    @GetMapping("/logs")
    public Object getLogs(@RequestParam(name = "limit", defaultValue = "200") Integer limit) {
        return aiAdminProxyService.getLogs(limit);
    }
}
