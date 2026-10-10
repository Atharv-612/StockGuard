package com.stockguard;

import com.stockguard.entity.Product;
import com.stockguard.entity.Shop;
import com.stockguard.entity.User;
import com.stockguard.exception.BusinessException;
import com.stockguard.repository.ProductRepository;
import com.stockguard.repository.ShopRepository;
import com.stockguard.repository.UserRepository;
import com.stockguard.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    private Shop shop;
    private Product product;

    @BeforeEach
    void setUp() {
        shop = new Shop("Test Grocery", "Tester", "+91 9999999999", "tester@test.com", "Test Street");
        shop = shopRepository.save(shop);

        User user = new User();
        user.setEmail("tester@test.com");
        user.setPasswordHash("hashed_pw");
        user.setFullName("Tester");
        user.setShop(shop);
        user.setRole("OWNER");
        userRepository.save(user);

        // Set security context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, java.util.Collections.emptyList())
        );

        product = new Product();
        product.setShop(shop);
        product.setName("Test Butter");
        product.setCostPrice(new BigDecimal("100.00"));
        product.setSellingPrice(new BigDecimal("120.00"));
        product.setCurrentStock(20);
        product.setMinStockThreshold(5);
        product = productRepository.save(product);
    }

    @Test
    void testStockAdjustmentPositive() {
        inventoryService.adjustStock(product.getId(), 5, "Received extra delivery");
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(25, updated.getCurrentStock());
    }

    @Test
    void testStockAdjustmentNegative() {
        inventoryService.adjustStock(product.getId(), -5, "Damaged stock");
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(15, updated.getCurrentStock());
    }

    @Test
    void testNegativeStockPrevention() {
        assertThrows(BusinessException.class, () -> {
            inventoryService.adjustStock(product.getId(), -50, "Excessive subtraction");
        });
    }
}
