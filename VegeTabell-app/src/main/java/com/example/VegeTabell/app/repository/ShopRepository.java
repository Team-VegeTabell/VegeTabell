package com.example.VegeTabell.app.repository;

import com.example.VegeTabell.app.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findByUserId(Long userId);
}
