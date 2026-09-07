package com.example.VegeTabell.app.dto;

import com.example.VegeTabell.app.entity.Product;

import java.time.Instant;

/**
 * 商品一覧（買い手ホーム）の1カード分の表示用モデル。
 */
public record ProductSummary(
        Long id,
        String name,
        String imageUrl,
        String shopName,
        String categoryIcon,
        int normalPrice,
        int rescuePrice,
        int discountPercent,
        String remainingLabel
) {

    public static ProductSummary from(Product product, Instant now) {
        int discountPercent = (int) Math.round(
                (1 - product.getRescuePrice() / (double) product.getNormalPrice()) * 100);

        return new ProductSummary(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getShop().getShopName(),
                product.getCategory().getIcon(),
                product.getNormalPrice(),
                product.getRescuePrice(),
                discountPercent,
                ExpiryPresenter.remainingLabel(product.getExpiryAt(), now)
        );
    }
}
