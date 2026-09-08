package com.example.VegeTabell.app.repository;

import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByShopIdOrderByCreatedAtDesc(Long shopId);

    long countByShopIdAndCreatedAtGreaterThanEqual(Long shopId, Instant since);

    @Query("SELECT p FROM Product p "
            + "WHERE p.status = :status AND p.remainingQuantity > 0 "
            + "AND (:categoryId IS NULL OR p.category.id = :categoryId) "
            + "AND (:keywordPattern IS NULL OR LOWER(p.name) LIKE :keywordPattern) "
            + "AND (:areaPattern IS NULL OR LOWER(p.shop.address) LIKE :areaPattern) "
            + "ORDER BY p.expiryAt ASC")
    List<Product> findAvailableForBuyer(@Param("status") ProductStatus status,
                                         @Param("categoryId") Long categoryId,
                                         @Param("keywordPattern") String keywordPattern,
                                         @Param("areaPattern") String areaPattern);

    List<Product> findByStatusInAndExpiryAtBefore(List<ProductStatus> statuses, Instant threshold);

    List<Product> findByStatusAndExpiryAtBetween(ProductStatus status, Instant from, Instant to);
}
