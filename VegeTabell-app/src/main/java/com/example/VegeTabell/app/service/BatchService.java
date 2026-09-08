package com.example.VegeTabell.app.service;

import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.type.NotificationType;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import com.example.VegeTabell.app.entity.type.ReservationStatus;
import com.example.VegeTabell.app.repository.NotificationRepository;
import com.example.VegeTabell.app.repository.ProductRepository;
import com.example.VegeTabell.app.repository.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class BatchService {

    // db-design.md/functional-requirements.mdに具体的な時間指定が無いため、以下はMVP暫定のしきい値。
    // pickup_start_atは予約時刻そのもの（ReservationService.reserve()参照）のため、
    // 「受取開始時刻の一定時間前」ではなく「受取期限（pickup_end_at）の一定時間前」を
    // リマインドのタイミングとして扱う（そうしないと予約直後にしか送れないため）。
    private static final Duration PICKUP_REMINDER_WINDOW = Duration.ofHours(1);
    private static final Duration STOCK_EXPIRING_WARNING_WINDOW = Duration.ofHours(2);

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public BatchService(ProductRepository productRepository,
                         ReservationRepository reservationRepository,
                         NotificationRepository notificationRepository,
                         NotificationService notificationService) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    // functional-requirements.md §2：expiry_atを過ぎた商品をexpiredにし、一覧から非表示にする。
    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void expireOverdueProducts() {
        Instant now = Instant.now();
        List<Product> overdue = productRepository.findByStatusInAndExpiryAtBefore(
                List.of(ProductStatus.ON_SALE, ProductStatus.SOLD_OUT), now);
        for (Product product : overdue) {
            product.setStatus(ProductStatus.EXPIRED);
            productRepository.save(product);
        }
    }

    // functional-requirements.md §5：受取期限を過ぎた予約を売り手の操作なしでcompletedにする。
    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void completeOverdueReservations() {
        Instant now = Instant.now();
        List<Reservation> overdue = reservationRepository.findByStatusAndPickupEndAtBefore(
                ReservationStatus.RESERVED, now);
        for (Reservation reservation : overdue) {
            reservation.setStatus(ReservationStatus.COMPLETED);
            reservationRepository.save(reservation);
        }
    }

    // db-design.md：受取期限が近い未受取の予約について、買い手にリマインド通知を送る。
    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void sendPickupReminders() {
        Instant now = Instant.now();
        List<Reservation> upcoming = reservationRepository.findByStatusAndPickupEndAtBetween(
                ReservationStatus.RESERVED, now, now.plus(PICKUP_REMINDER_WINDOW));
        for (Reservation reservation : upcoming) {
            if (notificationRepository.existsByReservationIdAndType(
                    reservation.getId(), NotificationType.PICKUP_REMINDER)) {
                continue;
            }
            Product product = reservation.getProduct();
            notificationService.create(reservation.getBuyer(), NotificationType.PICKUP_REMINDER,
                    "受け取り期限が近づいています", product.getName() + "の受け取り期限が近づいています",
                    product, reservation);
        }
    }

    // db-design.md：期限が近い（例：2時間前）かつ在庫ありの商品を、出品者に警告通知する。
    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void sendStockExpiringWarnings() {
        Instant now = Instant.now();
        List<Product> expiringSoon = productRepository.findByStatusAndExpiryAtBetween(
                ProductStatus.ON_SALE, now, now.plus(STOCK_EXPIRING_WARNING_WINDOW));
        for (Product product : expiringSoon) {
            if (notificationRepository.existsByProductIdAndType(
                    product.getId(), NotificationType.STOCK_EXPIRING_WARNING)) {
                continue;
            }
            notificationService.create(product.getShop().getUser(), NotificationType.STOCK_EXPIRING_WARNING,
                    "在庫が売れ残っています", product.getName() + "の期限が近づいています",
                    product, null);
        }
    }
}
