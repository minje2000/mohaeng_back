package org.poolpool.mohaeng.ai.chat.admin.controller;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.ai.chat.admin.service.AiAdminProxyService;
import org.poolpool.mohaeng.ai.chat.contact.service.AdminContactService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiSupportController {

    private final AiAdminProxyService aiAdminProxyService;
    private final AdminContactService adminContactService;

    @GetMapping("/contacts")
    public Object getContacts(@RequestParam(name = "limit", defaultValue = "200") Integer limit) {
        return adminContactService.listForAdmin(limit == null ? 200 : limit);
    }

    @PutMapping("/contacts/{itemId}")
    public Object updateContact(@PathVariable("itemId") String itemId, @RequestBody Map<String, Object> payload) {
        return adminContactService.update(itemId, payload);
    }

    @DeleteMapping("/contacts/{itemId}")
    public Object deleteContact(@PathVariable("itemId") String itemId) {
        return adminContactService.delete(itemId);
    }

    @PostMapping("/contacts/{itemId}/delete")
    public Object deleteContactPost(@PathVariable("itemId") String itemId) {
        return adminContactService.delete(itemId);
    }

    @GetMapping("/logs")
    public Object getLogs(@RequestParam(name = "limit", defaultValue = "200") Integer limit) {
        return aiAdminProxyService.getLogs(limit);
    }
}
