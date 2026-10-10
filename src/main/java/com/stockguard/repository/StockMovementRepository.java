package com.stockguard.repository;

import com.stockguard.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByShopIdOrderByCreatedAtDesc(Long shopId);
    List<StockMovement> findTop20ByShopIdOrderByCreatedAtDesc(Long shopId);
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
}
