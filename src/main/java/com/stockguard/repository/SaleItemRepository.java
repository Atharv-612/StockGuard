package com.stockguard.repository;

import com.stockguard.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);

    @Query("SELECT si.product.id, si.productName, SUM(si.quantity), SUM(si.totalPrice) " +
           "FROM SaleItem si WHERE si.sale.shop.id = :shopId AND si.sale.saleDate >= :start AND si.sale.saleDate <= :end " +
           "GROUP BY si.product.id, si.productName ORDER BY SUM(si.quantity) DESC")
    List<Object[]> findTopSellingProducts(@Param("shopId") Long shopId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
