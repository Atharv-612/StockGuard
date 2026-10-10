package com.stockguard.repository;

import com.stockguard.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByShopIdOrderByCreatedAtDesc(Long shopId);
    List<NotificationLog> findTop50ByShopIdOrderByCreatedAtDesc(Long shopId);
}
