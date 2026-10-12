package com.stockguard;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationSecurityTest {

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
    private ObjectMapper objectMapper;

    private Shop shop;
    private User owner;
    private User manager;
    private User staff;

    private String ownerToken;
    private String managerToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        shop = new Shop("Role Shop", "Owner Role", "+91 8888888888", "role@shop.com", "Role City");
        shop = shopRepository.save(shop);

        owner = createUser("owner_rbac@shop.com", "SHOP_OWNER");
        manager = createUser("manager_rbac@shop.com", "MANAGER");
        staff = createUser("staff_rbac@shop.com", "STAFF");

        ownerToken = jwtService.generateToken(owner.getEmail(), shop.getId(), owner.getRole());
        managerToken = jwtService.generateToken(manager.getEmail(), shop.getId(), manager.getRole());
        staffToken = jwtService.generateToken(staff.getEmail(), shop.getId(), staff.getRole());
    }

    private User createUser(String email, String role) {
        User u = new User();
        u.setShop(shop);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Pass123!"));
        u.setFullName(role + " User");
        u.setRole(role);
        u.setActive(true);
        return userRepository.save(u);
    }

    @Test
    void testShopOwnerCanUpdateShopSettings() throws Exception {
        mockMvc.perform(put("/api/settings/shop")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated Role Store"))))
                .andExpect(status().isOk());
    }

    @Test
    void testManagerCannotUpdateShopSettings() throws Exception {
        mockMvc.perform(put("/api/settings/shop")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hacked Store"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testStaffCannotCreateProduct() throws Exception {
        ProductDto.ProductRequest req = new ProductDto.ProductRequest();
        req.setName("Restricted Product");
        req.setCostPrice(BigDecimal.valueOf(10));
        req.setSellingPrice(BigDecimal.valueOf(20));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testStaffCanQueryProductsAndProcessSales() throws Exception {
        // Staff can read products
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        // Staff can read active alerts
        mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());
    }

    @Test
    void testStaffCannotAdjustInventory() throws Exception {
        mockMvc.perform(post("/api/inventory/adjust")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", 1L,
                                "quantityDifference", 100,
                                "reason", "Unauthorized staff adjustment"
                        ))))
                .andExpect(status().isForbidden());
    }
}
