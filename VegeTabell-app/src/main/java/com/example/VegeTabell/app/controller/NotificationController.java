package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.dto.NotificationView;
import com.example.VegeTabell.app.entity.Notification;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.repository.NotificationRepository;
import com.example.VegeTabell.app.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public String list(Model model, @AuthenticationPrincipal CustomUserDetails principal) {
        model.addAttribute("notifications", notificationRepository
                .findByUserIdOrderByCreatedAtDesc(principal.getUser().getId())
                .stream()
                .map(NotificationView::from)
                .toList());
        // "/"は買い手専用のためSecurityConfig上403になる。ロールに応じた戻り先をここで解決する。
        model.addAttribute("homeUrl", principal.getUser().getRole() == UserRole.SELLER
                ? "/seller/dashboard" : "/products");
        model.addAttribute("navRole", principal.getUser().getRole());
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String read(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!notification.getUser().getId().equals(principal.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        notification.setRead(true);
        notificationRepository.save(notification);

        Reservation reservation = notification.getReservation();
        if (reservation != null) {
            return notification.getUser().getRole() == UserRole.SELLER
                    ? "redirect:/seller/dashboard"
                    : "redirect:/reservations/" + reservation.getId();
        }
        return "redirect:/notifications";
    }
}
