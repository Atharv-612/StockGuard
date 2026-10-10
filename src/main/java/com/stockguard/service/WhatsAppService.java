package com.stockguard.service;

import com.stockguard.dto.NotificationDto;
import com.stockguard.entity.NotificationLog;
import com.stockguard.entity.NotificationPreference;
import com.stockguard.entity.Shop;
import com.stockguard.exception.BusinessException;
import com.stockguard.repository.NotificationLogRepository;
import com.stockguard.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    @Value("${stockguard.whatsapp.api-token:}")
    private String apiToken;

    @Value("${stockguard.whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${stockguard.whatsapp.mock-mode:true}")
    private boolean mockMode;

    @Value("${stockguard.whatsapp.api-version:v18.0}")
    private String apiVersion;

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final AuthService authService;
    private final RestTemplate restTemplate = new RestTemplate();

    public WhatsAppService(NotificationLogRepository notificationLogRepository,
                           NotificationPreferenceRepository preferenceRepository,
                           AuthService authService) {
        this.notificationLogRepository = notificationLogRepository;
        this.preferenceRepository = preferenceRepository;
        this.authService = authService;
    }

    public NotificationDto.NotificationPreferenceDto getPreferences() {
        Shop shop = authService.getCurrentUserShop();
        NotificationPreference pref = preferenceRepository.findByShopId(shop.getId())
                .orElseGet(() -> {
                    NotificationPreference np = new NotificationPreference();
                    np.setShop(shop);
                    np.setWhatsappPhone(shop.getPhone());
                    return preferenceRepository.save(np);
                });

        NotificationDto.NotificationPreferenceDto dto = new NotificationDto.NotificationPreferenceDto();
        dto.setEnableLowStock(pref.getEnableLowStock());
        dto.setEnableExpiry(pref.getEnableExpiry());
        dto.setEnableDailySales(pref.getEnableDailySales());
        dto.setWhatsappPhone(pref.getWhatsappPhone());
        dto.setExpiryWarningDays(pref.getExpiryWarningDays());
        dto.setMetaApiToken(pref.getMetaApiToken() != null && !pref.getMetaApiToken().isBlank() ? pref.getMetaApiToken() : this.apiToken);
        dto.setPhoneNumberId(pref.getPhoneNumberId() != null && !pref.getPhoneNumberId().isBlank() ? pref.getPhoneNumberId() : this.phoneNumberId);
        dto.setBusinessAccountId(pref.getBusinessAccountId());
        dto.setUseLiveApi(pref.getUseLiveApi() != null ? pref.getUseLiveApi() : (!mockMode && this.apiToken != null && !this.apiToken.isBlank()));
        return dto;
    }

    @Transactional
    public NotificationDto.NotificationPreferenceDto updatePreferences(NotificationDto.NotificationPreferenceDto dto) {
        Shop shop = authService.getCurrentUserShop();
        NotificationPreference pref = preferenceRepository.findByShopId(shop.getId())
                .orElseGet(() -> {
                    NotificationPreference np = new NotificationPreference();
                    np.setShop(shop);
                    return np;
                });

        if (dto.getEnableLowStock() != null) pref.setEnableLowStock(dto.getEnableLowStock());
        if (dto.getEnableExpiry() != null) pref.setEnableExpiry(dto.getEnableExpiry());
        if (dto.getEnableDailySales() != null) pref.setEnableDailySales(dto.getEnableDailySales());
        if (dto.getWhatsappPhone() != null) pref.setWhatsappPhone(dto.getWhatsappPhone());
        if (dto.getExpiryWarningDays() != null) {
            pref.setExpiryWarningDays(dto.getExpiryWarningDays());
            shop.setExpiryWarningDays(dto.getExpiryWarningDays());
        }
        if (dto.getMetaApiToken() != null) pref.setMetaApiToken(dto.getMetaApiToken());
        if (dto.getPhoneNumberId() != null) pref.setPhoneNumberId(dto.getPhoneNumberId());
        if (dto.getBusinessAccountId() != null) pref.setBusinessAccountId(dto.getBusinessAccountId());
        if (dto.getUseLiveApi() != null) pref.setUseLiveApi(dto.getUseLiveApi());

        NotificationPreference saved = preferenceRepository.save(pref);
        NotificationDto.NotificationPreferenceDto res = new NotificationDto.NotificationPreferenceDto();
        res.setEnableLowStock(saved.getEnableLowStock());
        res.setEnableExpiry(saved.getEnableExpiry());
        res.setEnableDailySales(saved.getEnableDailySales());
        res.setWhatsappPhone(saved.getWhatsappPhone());
        res.setExpiryWarningDays(saved.getExpiryWarningDays());
        res.setMetaApiToken(saved.getMetaApiToken());
        res.setPhoneNumberId(saved.getPhoneNumberId());
        res.setBusinessAccountId(saved.getBusinessAccountId());
        res.setUseLiveApi(saved.getUseLiveApi());
        return res;
    }

    public List<NotificationDto.NotificationLogDto> getNotificationLogs() {
        Shop shop = authService.getCurrentUserShop();
        return notificationLogRepository.findTop50ByShopIdOrderByCreatedAtDesc(shop.getId()).stream()
                .map(this::mapLog)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDto.NotificationLogDto sendNotification(NotificationDto.SendNotificationRequest request) {
        Shop shop = authService.getCurrentUserShop();
        String recipient = request.getRecipientPhone();

        NotificationPreference pref = preferenceRepository.findByShopId(shop.getId()).orElse(null);

        if (recipient == null || recipient.trim().isEmpty()) {
            if (pref != null && pref.getWhatsappPhone() != null && !pref.getWhatsappPhone().trim().isEmpty()) {
                recipient = pref.getWhatsappPhone();
            } else {
                recipient = shop.getPhone();
            }
        }

        if (recipient == null || recipient.trim().isEmpty()) {
            throw new BusinessException("Recipient phone number is required. Please set in WhatsApp Settings.");
        }

        String template = request.getTemplateName() != null ? request.getTemplateName() : "CUSTOM_ALERT";
        String messageBody = buildTemplateMessage(template, request.getCustomMessage(), shop);

        NotificationLog nLog = new NotificationLog();
        nLog.setShop(shop);
        nLog.setRecipientPhone(recipient);
        nLog.setTemplateName(template);
        nLog.setMessageContent(messageBody);

        // Check live configuration from DB preferences with fallback to application.properties
        boolean isLiveModeConfigured = (pref != null && Boolean.TRUE.equals(pref.getUseLiveApi()))
                || (!mockMode && (pref == null || pref.getUseLiveApi() == null));

        String activeToken = (pref != null && pref.getMetaApiToken() != null && !pref.getMetaApiToken().isBlank())
                ? pref.getMetaApiToken().trim()
                : this.apiToken;

        String activePhoneId = (pref != null && pref.getPhoneNumberId() != null && !pref.getPhoneNumberId().isBlank())
                ? pref.getPhoneNumberId().trim()
                : this.phoneNumberId;

        boolean isLiveApi = isLiveModeConfigured && activeToken != null && !activeToken.isBlank() && activePhoneId != null && !activePhoneId.isBlank();

        if (isLiveApi) {
            try {
                String metaUrl = "https://graph.facebook.com/" + apiVersion + "/" + activePhoneId + "/messages";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(activeToken);

                Map<String, Object> payload = new HashMap<>();
                payload.put("messaging_product", "whatsapp");
                payload.put("to", cleanPhoneNumber(recipient));

                if ("META_HELLO_WORLD".equalsIgnoreCase(template)) {
                    Map<String, Object> templateMap = new HashMap<>();
                    templateMap.put("name", "hello_world");
                    templateMap.put("language", Map.of("code", "en_US"));
                    payload.put("type", "template");
                    payload.put("template", templateMap);
                } else {
                    payload.put("type", "text");
                    payload.put("text", Map.of("body", messageBody));
                }

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(metaUrl, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    nLog.setStatus("SENT");
                    nLog.setProviderResponse("Meta Cloud API Success (HTTP " + response.getStatusCode() + "): " + response.getBody());
                } else {
                    nLog.setStatus("FAILED");
                    nLog.setProviderResponse("Meta Cloud API Error: " + response.getBody());
                }
            } catch (org.springframework.web.client.HttpStatusCodeException ex) {
                String errorBody = ex.getResponseBodyAsString();
                log.error("Failed to send WhatsApp message via Meta Cloud API HTTP {}: {}", ex.getStatusCode(), errorBody);
                nLog.setStatus("FAILED");
                nLog.setProviderResponse("Meta Cloud API " + ex.getStatusCode() + ": " + errorBody);
            } catch (Exception ex) {
                log.error("Failed to send WhatsApp message via Meta Cloud API: {}", ex.getMessage());
                nLog.setStatus("FAILED");
                nLog.setProviderResponse("Network/Connection Error: " + ex.getMessage());
            }
        } else {
            // Local Development Mock Mode
            nLog.setStatus("SIMULATED");
            if (!isLiveModeConfigured) {
                nLog.setProviderResponse("Simulated locally via StockGuard Dev Mock Engine. (Live API mode is switched OFF in WhatsApp Cloud Settings).");
            } else {
                nLog.setProviderResponse("Simulated locally. (Live API mode is ON, but Meta Access Token or Phone Number ID is missing).");
            }
        }

        NotificationLog savedLog = notificationLogRepository.save(nLog);
        return mapLog(savedLog);
    }

    private String buildTemplateMessage(String template, String customText, Shop shop) {
        String shopName = shop.getName();
        switch (template) {
            case "META_HELLO_WORLD":
                return "👋 [Meta Official Template: hello_world]\nTest message sent via Meta Cloud API verified gateway.";
            case "EXPIRY_WARNING":
                return "⚠️ [StockGuard Expiry Warning]\nHello " + shopName + "!\nAttention: Products in your store are nearing expiry within " + shop.getExpiryWarningDays() + " days.\nPlease inspect batch inventory to initiate clearance discounts or returns.\n" + (customText != null ? "\nDetails: " + customText : "");
            case "EXPIRED_ACTION":
                return "🚨 [StockGuard Critical Alert - Expired Stock]\nShop: " + shopName + "\nCritical: Certain stock batches have reached their expiry date and must NOT be sold!\nPlease quarantine or discard these batches immediately.\n" + (customText != null ? "\nDetails: " + customText : "");
            case "LOW_STOCK":
                return "📉 [StockGuard Low Stock Notification]\nShop: " + shopName + "\nSeveral inventory items have fallen below your minimum stock threshold.\nCheck your inventory dashboard to reorder with suppliers.\n" + (customText != null ? "\nDetails: " + customText : "");
            case "DAILY_SUMMARY":
                return "📊 [StockGuard Daily Summary]\nShop: " + shopName + "\nYour daily inventory & sales summary is ready. Review today's transactions and stock movements in StockGuard dashboard.\n" + (customText != null ? "\nDetails: " + customText : "");
            default:
                return "📢 [StockGuard Notification]\nShop: " + shopName + "\n" + (customText != null ? customText : "Inventory status update.");
        }
    }

    private String cleanPhoneNumber(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9]", "");
        // Prepend 91 for 10-digit Indian mobile numbers if not present
        if (cleaned.length() == 10 && (cleaned.startsWith("6") || cleaned.startsWith("7") || cleaned.startsWith("8") || cleaned.startsWith("9"))) {
            return "91" + cleaned;
        }
        return cleaned;
    }

    private NotificationDto.NotificationLogDto mapLog(NotificationLog log) {
        NotificationDto.NotificationLogDto dto = new NotificationDto.NotificationLogDto();
        dto.setId(log.getId());
        dto.setRecipientPhone(log.getRecipientPhone());
        dto.setTemplateName(log.getTemplateName());
        dto.setMessageContent(log.getMessageContent());
        dto.setStatus(log.getStatus());
        dto.setProviderResponse(log.getProviderResponse());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
