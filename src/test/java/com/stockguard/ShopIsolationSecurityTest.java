package com.stockguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockguard.dto.ProductDto;
import com.stockguard.entity.Category;
import com.stockguard.entity.Product;
import com.stockguard.entity.Shop;
import com.stockguard.entity.User;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.repository.ProductRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShopIsolationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private Shop shopA;
    private Shop shopB;

    private User userA;
    private User userB;

    private String tokenA;
    private String tokenB;

    private Product productA;
    private Product productB;

    private Category categoryB;

    @BeforeEach
    void setUp() {
        shopA = shopRepository.save(new Shop("Shop Alpha", "Owner Alpha", "+91 1111111111", "alpha@test.com", "City Alpha"));
        shopB = shopRepository.save(new Shop("Shop Beta", "Owner Beta", "+91 2222222222", "beta@test.com", "City Beta"));

        userA = new User();
        userA.setShop(shopA);
        userA.setEmail("owner_alpha@test.com");
        userA.setPasswordHash(passwordEncoder.encode("Pass123!"));
        userA.setFullName("Alpha Owner");
        userA.setRole("SHOP_OWNER");
        userA.setActive(true);
        userA = userRepository.save(userA);

        userB = new User();
        userB.setShop(shopB);
        userB.setEmail("owner_beta@test.com");
        userB.setPasswordHash(passwordEncoder.encode("Pass123!"));
        userB.setFullName("Beta Owner");
        userB.setRole("SHOP_OWNER");
        userB.setActive(true);
        userB = userRepository.save(userB);

        tokenA = jwtService.generateToken(userA.getEmail(), shopA.getId(), userA.getRole());
        tokenB = jwtService.generateToken(userB.getEmail(), shopB.getId(), userB.getRole());

        // Create product for Shop A
        productA = new Product();
        productA.setShop(shopA);
        productA.setName("Alpha Special Milk");
        productA.setCostPrice(BigDecimal.valueOf(25));
        productA.setSellingPrice(BigDecimal.valueOf(30));
        productA.setCurrentStock(50);
        productA = productRepository.save(productA);

        // Create product for Shop B
        productB = new Product();
        productB.setShop(shopB);
        productB.setName("Beta Special Cheese");
        productB.setCostPrice(BigDecimal.valueOf(100));
        productB.setSellingPrice(BigDecimal.valueOf(120));
        productB.setCurrentStock(20);
        productB = productRepository.save(productB);

        // Create category for Shop B
        categoryB = categoryRepository.save(new Category(shopB, "Beta Dairy Products", "Exclusive to Shop B", "fa-cheese"));
    }

    @Test
    void testUserCannotAccessProductOfAnotherShopById() throws Exception {
        // User B can access their own Product B
        mockMvc.perform(get("/api/products/" + productB.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productB.getId()));

        // User A attempts to read Product B -> Isolated and blocked
        mockMvc.perform(get("/api/products/" + productB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void testUserCannotDeleteProductOfAnotherShop() throws Exception {
        // User A attempts to delete Product B
        mockMvc.perform(delete("/api/products/" + productB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUserCannotInjectCategoryFromAnotherShop() throws Exception {
        // User A attempts to create a product in Shop A, but injects Category ID from Shop B
        ProductDto.ProductRequest req = new ProductDto.ProductRequest();
        req.setName("Alpha Product With Stolen Category");
        req.setCostPrice(BigDecimal.valueOf(10));
        req.setSellingPrice(BigDecimal.valueOf(20));
        req.setCategoryId(categoryB.getId()); // Belongs to Shop B!

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Category not found or does not belong to this shop")));
    }
}
