package com.example.VegeTabell.app.repository;

import com.example.VegeTabell.app.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByShopIdOrderByCreatedAtDesc(Long shopId);

    long countByShopIdAndCreatedAtGreaterThanEqual(Long shopId, Instant since);
}
