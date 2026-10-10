package com.stockguard.dto;

import java.time.LocalDateTime;

public class NotificationDto {

    public static class NotificationPreferenceDto {
        private Boolean enableLowStock;
        private Boolean enableExpiry;
        private Boolean enableDailySales;
        private String whatsappPhone;
        private Integer expiryWarningDays;
        private String metaApiToken;
        private String phoneNumberId;
        private String businessAccountId;
        private Boolean useLiveApi;

        public Boolean getEnableLowStock() { return enableLowStock; }
        public void setEnableLowStock(Boolean enableLowStock) { this.enableLowStock = enableLowStock; }
        public Boolean getEnableExpiry() { return enableExpiry; }
        public void setEnableExpiry(Boolean enableExpiry) { this.enableExpiry = enableExpiry; }
        public Boolean getEnableDailySales() { return enableDailySales; }
        public void setEnableDailySales(Boolean enableDailySales) { this.enableDailySales = enableDailySales; }
        public String getWhatsappPhone() { return whatsappPhone; }
        public void setWhatsappPhone(String whatsappPhone) { this.whatsappPhone = whatsappPhone; }
        public Integer getExpiryWarningDays() { return expiryWarningDays; }
        public void setExpiryWarningDays(Integer expiryWarningDays) { this.expiryWarningDays = expiryWarningDays; }
        public String getMetaApiToken() { return metaApiToken; }
        public void setMetaApiToken(String metaApiToken) { this.metaApiToken = metaApiToken; }
        public String getPhoneNumberId() { return phoneNumberId; }
        public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
        public String getBusinessAccountId() { return businessAccountId; }
        public void setBusinessAccountId(String businessAccountId) { this.businessAccountId = businessAccountId; }
        public Boolean getUseLiveApi() { return useLiveApi; }
        public void setUseLiveApi(Boolean useLiveApi) { this.useLiveApi = useLiveApi; }
    }

    public static class SendNotificationRequest {
        private String templateName;
        private String recipientPhone;
        private String customMessage;

        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getRecipientPhone() { return recipientPhone; }
        public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
        public String getCustomMessage() { return customMessage; }
        public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }
    }

    public static class NotificationLogDto {
        private Long id;
        private String recipientPhone;
        private String templateName;
        private String messageContent;
        private String status;
        private String providerResponse;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getRecipientPhone() { return recipientPhone; }
        public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getMessageContent() { return messageContent; }
        public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getProviderResponse() { return providerResponse; }
        public void setProviderResponse(String providerResponse) { this.providerResponse = providerResponse; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
