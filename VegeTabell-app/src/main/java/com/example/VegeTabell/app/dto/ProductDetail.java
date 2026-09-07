package com.example.VegeTabell.app.dto;

import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.type.ProductStatus;

import java.time.Instant;

/**
 * 商品詳細画面の表示用モデル。
 */
public record ProductDetail(
        Long id,
        String name,
        String description,
        String imageUrl,
        String shopName,
        String shopAddress,
        int normalPrice,
        int rescuePrice,
        int savingsAmount,
        int remainingQuantity,
        String remainingLabel,
        boolean urgent,
        boolean reservable,
        String statusLabel
) {

    public static ProductDetail from(Product product, Instant now) {
        boolean reservable = product.getStatus() == ProductStatus.ON_SALE
                && product.getRemainingQuantity() > 0;

        return new ProductDetail(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getImageUrl(),
                product.getShop().getShopName(),
                product.getShop().getAddress(),
                product.getNormalPrice(),
                product.getRescuePrice(),
                product.getNormalPrice() - product.getRescuePrice(),
                product.getRemainingQuantity(),
                ExpiryPresenter.remainingLabel(product.getExpiryAt(), now),
                ExpiryPresenter.isUrgent(product.getExpiryAt(), now),
                reservable,
                statusLabel(product.getStatus())
        );
    }

    private static String statusLabel(ProductStatus status) {
        return switch (status) {
            case ON_SALE -> "出品中";
            case SOLD_OUT -> "完売しました";
            case EXPIRED -> "受付を終了しました";
        };
    }
}
