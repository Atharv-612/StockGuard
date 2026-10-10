package com.stockguard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false, unique = true)
    private Shop shop;

    @Column(name = "enable_low_stock")
    private Boolean enableLowStock = true;

    @Column(name = "enable_expiry")
    private Boolean enableExpiry = true;

    @Column(name = "enable_daily_sales")
    private Boolean enableDailySales = true;

    @Column(name = "whatsapp_phone", length = 30)
    private String whatsappPhone;

    @Column(name = "expiry_warning_days")
    private Integer expiryWarningDays = 7;

    @Column(name = "meta_api_token", length = 500)
    private String metaApiToken;

    @Column(name = "phone_number_id", length = 100)
    private String phoneNumberId;

    @Column(name = "business_account_id", length = 100)
    private String businessAccountId;

    @Column(name = "use_live_api")
    private Boolean useLiveApi = false;

    public NotificationPreference() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }

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

    public Boolean getUseLiveApi() { return useLiveApi != null ? useLiveApi : false; }
    public void setUseLiveApi(Boolean useLiveApi) { this.useLiveApi = useLiveApi; }
}
