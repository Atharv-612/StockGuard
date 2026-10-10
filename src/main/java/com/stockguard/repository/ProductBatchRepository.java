package com.stockguard.repository;

import com.stockguard.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    List<ProductBatch> findByProductIdOrderByExpiryDateAsc(Long productId);

    Optional<ProductBatch> findByIdAndProductShopId(Long id, Long shopId);

    @Query("SELECT b FROM ProductBatch b WHERE b.product.shop.id = :shopId ORDER BY b.expiryDate ASC")
    List<ProductBatch> findAllByShopId(@Param("shopId") Long shopId);

    @Query("SELECT b FROM ProductBatch b WHERE b.product.id = :productId AND b.currentQuantity > 0 ORDER BY b.expiryDate ASC")
    List<ProductBatch> findAvailableBatchesForSale(@Param("productId") Long productId);

    @Query("SELECT b FROM ProductBatch b WHERE b.product.shop.id = :shopId AND b.expiryDate < :today AND b.currentQuantity > 0")
    List<ProductBatch> findExpiredBatches(@Param("shopId") Long shopId, @Param("today") LocalDate today);

    @Query("SELECT b FROM ProductBatch b WHERE b.product.shop.id = :shopId AND b.expiryDate >= :today AND b.expiryDate <= :warningDate AND b.currentQuantity > 0")
    List<ProductBatch> findExpiringSoonBatches(@Param("shopId") Long shopId, @Param("today") LocalDate today, @Param("warningDate") LocalDate warningDate);

    @Query("SELECT COUNT(b) FROM ProductBatch b WHERE b.product.shop.id = :shopId AND b.expiryDate < :today AND b.currentQuantity > 0")
    long countExpiredBatches(@Param("shopId") Long shopId, @Param("today") LocalDate today);

    @Query("SELECT COUNT(b) FROM ProductBatch b WHERE b.product.shop.id = :shopId AND b.expiryDate >= :today AND b.expiryDate <= :warningDate AND b.currentQuantity > 0")
    long countExpiringSoonBatches(@Param("shopId") Long shopId, @Param("today") LocalDate today, @Param("warningDate") LocalDate warningDate);
}
