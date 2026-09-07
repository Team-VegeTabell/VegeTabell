package com.example.VegeTabell.app.service;

import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.CanceledBy;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import com.example.VegeTabell.app.entity.type.ReservationStatus;
import com.example.VegeTabell.app.repository.ProductRepository;
import com.example.VegeTabell.app.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;

    public ReservationService(ReservationRepository reservationRepository, ProductRepository productRepository) {
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
    }

    // 呼び出し元（ReservationController）が在庫・ステータスの妥当性チェック済みであることを前提とする。
    // 同時予約時の在庫整合性はdb-design.mdの未確定事項のため、MVPでは楽観・悲観ロックは行わない。
    @Transactional
    public Reservation reserve(Product product, User buyer, int quantity) {
        product.setRemainingQuantity(product.getRemainingQuantity() - quantity);
        if (product.getRemainingQuantity() == 0) {
            product.setStatus(ProductStatus.SOLD_OUT);
        }
        productRepository.save(product);

        Reservation reservation = new Reservation();
        reservation.setProduct(product);
        reservation.setBuyer(buyer);
        reservation.setQuantity(quantity);
        reservation.setTotalPrice(product.getRescuePrice() * quantity);
        Instant now = Instant.now();
        // 受取可能時間帯を決めるルールがdocsに未定義のため、予約時刻〜商品のexpiry_atをMVP暫定の受取可能時間帯とする。
        reservation.setPickupStartAt(now);
        reservation.setPickupEndAt(product.getExpiryAt());
        reservation.setStatus(ReservationStatus.RESERVED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void cancel(Reservation reservation, CanceledBy canceledBy) {
        reservation.setStatus(ReservationStatus.CANCELED);
        reservation.setCanceledBy(canceledBy);
        reservation.setCanceledAt(Instant.now());
        reservationRepository.save(reservation);

        Product product = reservation.getProduct();
        product.setRemainingQuantity(product.getRemainingQuantity() + reservation.getQuantity());
        if (product.getStatus() == ProductStatus.SOLD_OUT && product.getExpiryAt().isAfter(Instant.now())) {
            product.setStatus(ProductStatus.ON_SALE);
        }
        productRepository.save(product);
    }
}
