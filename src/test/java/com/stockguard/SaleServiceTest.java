package com.stockguard;

import com.stockguard.dto.SaleDto;
import com.stockguard.entity.Product;
import com.stockguard.entity.Shop;
import com.stockguard.entity.User;
import com.stockguard.exception.BusinessException;
import com.stockguard.repository.ProductRepository;
import com.stockguard.repository.ShopRepository;
import com.stockguard.repository.UserRepository;
import com.stockguard.service.SaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SaleServiceTest {

    @Autowired
    private SaleService saleService;

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
        shop = new Shop("Test Store", "Retailer", "+91 8888888888", "retailer@test.com", "City Center");
        shop = shopRepository.save(shop);

        User user = new User();
        user.setEmail("retailer@test.com");
        user.setPasswordHash("hashed_pw");
        user.setFullName("Retailer");
        user.setShop(shop);
        user.setRole("OWNER");
        userRepository.save(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, java.util.Collections.emptyList())
        );

        product = new Product();
        product.setShop(shop);
        product.setName("Fresh Milk 1L");
        product.setCostPrice(new BigDecimal("50.00"));
        product.setSellingPrice(new BigDecimal("60.00"));
        product.setCurrentStock(10);
        product.setMinStockThreshold(3);
        product = productRepository.save(product);
    }

    @Test
    void testSuccessfulSaleAndStockDeduction() {
        SaleDto.SaleItemRequest itemReq = new SaleDto.SaleItemRequest();
        itemReq.setProductId(product.getId());
        itemReq.setQuantity(4);

        SaleDto.SaleRequest saleReq = new SaleDto.SaleRequest();
        saleReq.setCustomerName("Pooja");
        saleReq.setPaymentMethod("UPI");
        saleReq.setItems(List.of(itemReq));

        SaleDto.SaleResponse response = saleService.processSale(saleReq);

        assertNotNull(response.getInvoiceNumber());
        assertEquals(new BigDecimal("240.00"), response.getTotalAmount());

        Product updatedProd = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(6, updatedProd.getCurrentStock());
    }

    @Test
    void testInsufficientStockThrowsException() {
        SaleDto.SaleItemRequest itemReq = new SaleDto.SaleItemRequest();
        itemReq.setProductId(product.getId());
        itemReq.setQuantity(15); // Exceeds available 10

        SaleDto.SaleRequest saleReq = new SaleDto.SaleRequest();
        saleReq.setItems(List.of(itemReq));

        assertThrows(BusinessException.class, () -> {
            saleService.processSale(saleReq);
        });
    }
}
