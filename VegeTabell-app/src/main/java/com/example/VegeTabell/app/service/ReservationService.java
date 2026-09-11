package com.example.VegeTabell.app.service;

import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.CanceledBy;
import com.example.VegeTabell.app.entity.type.NotificationType;
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
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository,
                               ProductRepository productRepository,
                               NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
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
        Reservation saved = reservationRepository.save(reservation);

        notificationService.create(buyer, NotificationType.RESERVATION_CONFIRMED, "予約が確定しました",
                product.getName() + "を予約しました", product, saved);
        notificationService.create(product.getShop().getUser(), NotificationType.NEW_RESERVATION, "新しい予約が入りました",
                buyer.getDisplayName() + "さんが" + product.getName() + "を予約しました", product, saved);

        return saved;
    }

    @Transactional
    public void complete(Reservation reservation) {
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);

        Product product = reservation.getProduct();
        notificationService.create(product.getShop().getUser(), NotificationType.PICKUP_COMPLETED,
                "商品が受け取られました",
                reservation.getBuyer().getDisplayName() + "さんが" + product.getName() + "を受け取りました",
                product, reservation);
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

        // キャンセルした側は自分の操作を把握済みのため、相手側にのみ通知する。
        User recipient = canceledBy == CanceledBy.BUYER
                ? product.getShop().getUser()
                : reservation.getBuyer();
        notificationService.create(recipient, NotificationType.RESERVATION_CANCELED, "予約がキャンセルされました",
                product.getName() + "の予約がキャンセルされました", product, reservation);
    }
}
