package com.stockguard.repository;

import com.stockguard.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Optional<Product> findByIdAndShopId(Long id, Long shopId);

    List<Product> findByShopIdAndCategoryIdOrderByCreatedAtDesc(Long shopId, Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchProducts(@Param("shopId") Long shopId, @Param("query") String query);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.currentStock <= p.minStockThreshold")
    List<Product> findLowStockProducts(@Param("shopId") Long shopId);

    long countByShopId(Long shopId);

    @Query("SELECT COALESCE(SUM(p.currentStock), 0) FROM Product p WHERE p.shop.id = :shopId")
    Long sumTotalStockQuantityByShopId(@Param("shopId") Long shopId);
}
