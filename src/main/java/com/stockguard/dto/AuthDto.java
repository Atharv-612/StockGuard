package com.stockguard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public LoginRequest() {}
        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        @NotBlank(message = "Shop name is required")
        private String shopName;

        @NotBlank(message = "Owner full name is required")
        private String ownerName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        private String phone;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        public RegisterRequest() {}

        public String getShopName() { return shopName; }
        public void setShopName(String shopName) { this.shopName = shopName; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private String token;
        private Long userId;
        private String email;
        private String fullName;
        private String role;
        private Long shopId;
        private String shopName;
        private String currency;

        public AuthResponse() {}

        public AuthResponse(String token, Long userId, String email, String fullName, String role, Long shopId, String shopName, String currency) {
            this.token = token;
            this.userId = userId;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
            this.shopId = shopId;
            this.shopName = shopName;
            this.currency = currency;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Long getShopId() { return shopId; }
        public void setShopId(Long shopId) { this.shopId = shopId; }
        public String getShopName() { return shopName; }
        public void setShopName(String shopName) { this.shopName = shopName; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class CreateUserRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Full name is required")
        private String fullName;

        private String phone;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotBlank(message = "Role is required (MANAGER or STAFF)")
        private String role; // MANAGER, STAFF

        public CreateUserRequest() {}

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class UserResponse {
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private String role;
        private Boolean active;

        public UserResponse() {}
        public UserResponse(Long id, String email, String fullName, String phone, String role, Boolean active) {
            this.id = id;
            this.email = email;
            this.fullName = fullName;
            this.phone = phone;
            this.role = role;
            this.active = active;
        }

        public Long getId() { return id; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getPhone() { return phone; }
        public String getRole() { return role; }
        public Boolean getActive() { return active; }
    }
}
