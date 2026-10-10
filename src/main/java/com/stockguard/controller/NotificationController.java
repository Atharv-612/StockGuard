package com.stockguard.controller;

import com.stockguard.dto.NotificationDto;
import com.stockguard.service.WhatsAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final WhatsAppService whatsAppService;

    public NotificationController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationDto.NotificationPreferenceDto> getPreferences() {
        return ResponseEntity.ok(whatsAppService.getPreferences());
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationDto.NotificationPreferenceDto> updatePreferences(
            @RequestBody NotificationDto.NotificationPreferenceDto dto
    ) {
        return ResponseEntity.ok(whatsAppService.updatePreferences(dto));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<NotificationDto.NotificationLogDto>> getNotificationLogs() {
        return ResponseEntity.ok(whatsAppService.getNotificationLogs());
    }

    @PostMapping("/send")
    public ResponseEntity<NotificationDto.NotificationLogDto> sendNotification(
            @RequestBody NotificationDto.SendNotificationRequest request
    ) {
        return ResponseEntity.ok(whatsAppService.sendNotification(request));
    }
}
