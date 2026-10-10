package com.stockguard.controller;

import com.stockguard.entity.Category;
import com.stockguard.entity.Shop;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final AuthService authService;

    public CategoryController(CategoryRepository categoryRepository, AuthService authService) {
        this.categoryRepository = categoryRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<Category>> getCategories() {
        Shop shop = authService.getCurrentUserShop();
        return ResponseEntity.ok(categoryRepository.findByShopIdOrderByNameAsc(shop.getId()));
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category categoryReq) {
        Shop shop = authService.getCurrentUserShop();
        Category cat = new Category(shop, categoryReq.getName(), categoryReq.getDescription(), categoryReq.getIcon());
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryRepository.save(cat));
    }
}
