package com.stockguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockguard.config.RateLimitingFilter;
import com.stockguard.dto.AuthDto;
import com.stockguard.dto.ProductDto;
import com.stockguard.entity.Shop;
import com.stockguard.entity.User;
import com.stockguard.repository.ShopRepository;
import com.stockguard.repository.UserRepository;
import com.stockguard.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InputValidationAndRateLimitSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private Shop shop;
    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        rateLimitingFilter.resetLimits();

        shop = shopRepository.save(new Shop("Validation Shop", "Val Owner", "+91 9999999999", "val@test.com", "Val City"));

        user = new User();
        user.setShop(shop);
        user.setEmail("val_user@test.com");
        user.setPasswordHash(passwordEncoder.encode("Pass123!"));
        user.setFullName("Validation Tester");
        user.setRole("SHOP_OWNER");
        user.setActive(true);
        user = userRepository.save(user);

        token = jwtService.generateToken(user.getEmail(), shop.getId(), user.getRole());
    }

    @Test
    void testSqlInjectionPayloadIsTreatedSafelyAsLiteral() throws Exception {
        // Attack payload attempting SQL injection in product search
        String sqlInjectionQuery = "' OR '1'='1' -- ";

        mockMvc.perform(get("/api/products")
                        .param("search", sqlInjectionQuery)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // Should safely return empty list or matching literal, not database leak or SQL syntax error
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testInvalidProductInputRejectedWithValidationErrors() throws Exception {
        ProductDto.ProductRequest req = new ProductDto.ProductRequest();
        req.setName(""); // Blank name violates @NotBlank
        req.setCostPrice(BigDecimal.valueOf(-5.0)); // Negative cost violates @DecimalMin
        req.setSellingPrice(BigDecimal.valueOf(10.0));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void testRateLimiterBlocksExcessiveLoginAttempts() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest("brute_force@test.com", "BadPassword");

        // Configured limit in test application.properties is 5
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt should be blocked by RateLimitingFilter with HTTP 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.status").value(429));
    }
}
