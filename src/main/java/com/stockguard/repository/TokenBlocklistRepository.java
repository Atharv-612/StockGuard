package com.stockguard.repository;

import com.stockguard.entity.TokenBlocklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface TokenBlocklistRepository extends JpaRepository<TokenBlocklist, Long> {

    boolean existsByJti(String jti);

    /** Clean up naturally expired tokens to keep the table small */
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenBlocklist t WHERE t.expiresAt < :now")
    int deleteExpiredBefore(@Param("now") LocalDateTime now);
}
