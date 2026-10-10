package com.stockguard.service;

import com.stockguard.dto.ProductDto;
import com.stockguard.entity.Category;
import com.stockguard.entity.Product;
import com.stockguard.entity.Shop;
import com.stockguard.exception.ResourceNotFoundException;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AuthService authService;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          AuthService authService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.authService = authService;
    }

    public List<ProductDto.ProductResponse> getAllProducts(String search, Long categoryId) {
        Shop shop = authService.getCurrentUserShop();
        List<Product> products;

        if (search != null && !search.trim().isEmpty()) {
            products = productRepository.searchProducts(shop.getId(), search.trim());
        } else if (categoryId != null) {
            products = productRepository.findByShopIdAndCategoryIdOrderByCreatedAtDesc(shop.getId(), categoryId);
        } else {
            products = productRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
        }

        return products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductDto.ProductResponse getProductById(Long id) {
        Shop shop = authService.getCurrentUserShop();
        Product product = productRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Transactional
    public ProductDto.ProductResponse createProduct(ProductDto.ProductRequest request) {
        Shop shop = authService.getCurrentUserShop();

        Product product = new Product();
        product.setShop(shop);
        product.setName(request.getName());
        product.setSku(request.getSku() != null ? request.getSku() : generateSku(request.getName()));
        product.setBarcode(request.getBarcode());
        product.setDescription(request.getDescription());
        product.setCostPrice(request.getCostPrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setCurrentStock(0); // Initial stock is 0 until batch or inventory adjustment is added
        product.setMinStockThreshold(request.getMinStockThreshold() != null ? request.getMinStockThreshold() : shop.getMinStockDefault());
        product.setUnit(request.getUnit() != null ? request.getUnit() : "pcs");

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Transactional
    public ProductDto.ProductResponse updateProduct(Long id, ProductDto.ProductRequest request) {
        Shop shop = authService.getCurrentUserShop();
        Product product = productRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(request.getName());
        if (request.getSku() != null) product.setSku(request.getSku());
        if (request.getBarcode() != null) product.setBarcode(request.getBarcode());
        product.setDescription(request.getDescription());
        product.setCostPrice(request.getCostPrice());
        product.setSellingPrice(request.getSellingPrice());
        if (request.getMinStockThreshold() != null) {
            product.setMinStockThreshold(request.getMinStockThreshold());
        }
        if (request.getUnit() != null) product.setUnit(request.getUnit());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Shop shop = authService.getCurrentUserShop();
        Product product = productRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    public ProductDto.ProductResponse mapToResponse(Product product) {
        ProductDto.ProductResponse resp = new ProductDto.ProductResponse();
        resp.setId(product.getId());
        resp.setShopId(product.getShop().getId());
        if (product.getCategory() != null) {
            resp.setCategoryId(product.getCategory().getId());
            resp.setCategoryName(product.getCategory().getName());
        }
        resp.setName(product.getName());
        resp.setSku(product.getSku());
        resp.setBarcode(product.getBarcode());
        resp.setDescription(product.getDescription());
        resp.setCostPrice(product.getCostPrice());
        resp.setSellingPrice(product.getSellingPrice());
        resp.setCurrentStock(product.getCurrentStock());
        resp.setMinStockThreshold(product.getMinStockThreshold());
        resp.setUnit(product.getUnit());
        resp.setIsLowStock(product.getCurrentStock() <= product.getMinStockThreshold());
        resp.setActiveBatchesCount(product.getBatches() != null ? product.getBatches().size() : 0);
        resp.setCreatedAt(product.getCreatedAt());
        return resp;
    }

    private String generateSku(String name) {
        String prefix = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (prefix.length() > 4) prefix = prefix.substring(0, 4);
        return prefix + "-" + System.currentTimeMillis() % 10000;
    }
}
