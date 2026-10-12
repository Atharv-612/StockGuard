package com.stockguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockguard.dto.AuthDto;
import com.stockguard.entity.Shop;
import com.stockguard.entity.User;
import com.stockguard.repository.ShopRepository;
import com.stockguard.repository.UserRepository;
import com.stockguard.service.JwtService;
import com.stockguard.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationSecurityTest {

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
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private ObjectMapper objectMapper;

    private Shop shop;
    private User user;

    @BeforeEach
    void setUp() {
        shop = new Shop("Auth Security Shop", "Sec Owner", "+91 9999999999", "sec@shop.com", "Sec City");
        shop = shopRepository.save(shop);

        user = new User();
        user.setShop(shop);
        user.setEmail("sec_auth_test@shop.com");
        user.setPasswordHash(passwordEncoder.encode("SecurePass123!"));
        user.setFullName("Security Tester");
        user.setRole("SHOP_OWNER");
        user.setActive(true);
        user = userRepository.save(user);
    }

    @Test
    void testSuccessfulLoginReturnsValidToken() throws Exception {
        AuthDto.LoginRequest login = new AuthDto.LoginRequest(user.getEmail(), "SecurePass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void testInvalidPasswordReturnsUnauthorized() throws Exception {
        AuthDto.LoginRequest login = new AuthDto.LoginRequest(user.getEmail(), "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testExpiredOrInvalidTokenIsRejected() throws Exception {
        // Manipulated token with garbage signature
        String tamperedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzZWNfYXV0aF90ZXN0QHNob3AuY29tIn0.tampered_signature";

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogoutBlacklistsTokenImmediately() throws Exception {
        String token = jwtService.generateToken(user.getEmail(), shop.getId(), user.getRole());

        // Token works initially
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // User calls logout
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Token is blacklisted in service
        assertTrue(tokenBlacklistService.isBlacklisted(token));

        // Subsequent API call with same token must be rejected with 401 Unauthorized
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
