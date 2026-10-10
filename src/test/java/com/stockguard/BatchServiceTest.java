package com.stockguard;

import com.stockguard.dto.BatchDto;
import com.stockguard.entity.Product;
import com.stockguard.entity.ProductBatch;
import com.stockguard.entity.Shop;
import com.stockguard.entity.User;
import com.stockguard.repository.ProductBatchRepository;
import com.stockguard.repository.ProductRepository;
import com.stockguard.repository.ShopRepository;
import com.stockguard.repository.UserRepository;
import com.stockguard.service.BatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BatchServiceTest {

    @Autowired
    private BatchService batchService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductBatchRepository batchRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    private Shop shop;
    private Product product;

    @BeforeEach
    void setUp() {
        shop = new Shop("Dairy Shop", "Dairy Owner", "+91 7777777777", "dairy@test.com", "Dairy Road");
        shop.setExpiryWarningDays(7);
        shop = shopRepository.save(shop);

        User user = new User();
        user.setEmail("dairy@test.com");
        user.setPasswordHash("hashed_pw");
        user.setFullName("Dairy Owner");
        user.setShop(shop);
        user.setRole("OWNER");
        userRepository.save(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, java.util.Collections.emptyList())
        );

        product = new Product();
        product.setShop(shop);
        product.setName("Fresh Curd 500g");
        product.setCostPrice(new BigDecimal("30.00"));
        product.setSellingPrice(new BigDecimal("40.00"));
        product.setCurrentStock(0);
        product.setMinStockThreshold(5);
        product = productRepository.save(product);
    }

    @Test
    void testCreateBatchUpdatesProductStock() {
        BatchDto.BatchRequest req = new BatchDto.BatchRequest();
        req.setProductId(product.getId());
        req.setBatchNumber("BATCH-CRD-TEST");
        req.setQuantity(25);
        req.setCostPrice(new BigDecimal("30.00"));
        req.setSellingPrice(new BigDecimal("40.00"));
        req.setMfgDate(LocalDate.now().minusDays(1));
        req.setExpiryDate(LocalDate.now().plusDays(15));

        BatchDto.BatchResponse response = batchService.createBatch(req);

        assertEquals("SAFE", response.getExpiryState());
        assertEquals(25, response.getCurrentQuantity());

        Product updatedProd = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(25, updatedProd.getCurrentStock());
    }

    @Test
    void testStrictExpiryStatusComputation() {
        // Expired batch
        ProductBatch expiredBatch = new ProductBatch();
        expiredBatch.setProduct(product);
        expiredBatch.setBatchNumber("EXPIRED-01");
        expiredBatch.setInitialQuantity(10);
        expiredBatch.setCurrentQuantity(10);
        expiredBatch.setCostPrice(new BigDecimal("30.00"));
        expiredBatch.setSellingPrice(new BigDecimal("40.00"));
        expiredBatch.setExpiryDate(LocalDate.now().minusDays(3)); // 3 days ago
        expiredBatch = batchRepository.save(expiredBatch);

        BatchDto.BatchResponse expResp = batchService.mapToResponse(expiredBatch, 7);
        assertEquals("EXPIRED", expResp.getExpiryState());
        assertNotEquals("SAFE", expResp.getExpiryState()); // Strict requirement: NEVER mark expired as safe!

        // Expiring soon batch (3 days ahead, within 7 day warning window)
        ProductBatch soonBatch = new ProductBatch();
        soonBatch.setProduct(product);
        soonBatch.setBatchNumber("SOON-01");
        soonBatch.setInitialQuantity(10);
        soonBatch.setCurrentQuantity(10);
        soonBatch.setCostPrice(new BigDecimal("30.00"));
        soonBatch.setSellingPrice(new BigDecimal("40.00"));
        soonBatch.setExpiryDate(LocalDate.now().plusDays(3));
        soonBatch = batchRepository.save(soonBatch);

        BatchDto.BatchResponse soonResp = batchService.mapToResponse(soonBatch, 7);
        assertEquals("EXPIRING_SOON", soonResp.getExpiryState());
    }

    @Test
    void testDiscardExpiredBatchRemovesStock() {
        // Batch with 10 units
        BatchDto.BatchRequest req = new BatchDto.BatchRequest();
        req.setProductId(product.getId());
        req.setBatchNumber("BATCH-DISCARD");
        req.setQuantity(10);
        req.setCostPrice(new BigDecimal("30.00"));
        req.setSellingPrice(new BigDecimal("40.00"));
        req.setExpiryDate(LocalDate.now().minusDays(1));
        BatchDto.BatchResponse created = batchService.createBatch(req);

        batchService.discardExpiredBatch(created.getId(), "Quarantined spoiled batch");

        ProductBatch batchAfter = batchRepository.findById(created.getId()).orElseThrow();
        assertEquals(0, batchAfter.getCurrentQuantity());
        assertEquals("DISCARDED", batchAfter.getStatus());

        Product prodAfter = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, prodAfter.getCurrentStock());
    }
}
