package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.dto.ProductDetail;
import com.example.VegeTabell.app.dto.ReservationView;
import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Reservation;
import com.example.VegeTabell.app.entity.Shop;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.CanceledBy;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import com.example.VegeTabell.app.entity.type.ReservationStatus;
import com.example.VegeTabell.app.form.ReservationForm;
import com.example.VegeTabell.app.repository.ProductRepository;
import com.example.VegeTabell.app.repository.ReservationRepository;
import com.example.VegeTabell.app.repository.ShopRepository;
import com.example.VegeTabell.app.security.CustomUserDetails;
import com.example.VegeTabell.app.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;

@Controller
public class ReservationController {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final ShopRepository shopRepository;
    private final ReservationService reservationService;

    public ReservationController(ProductRepository productRepository,
                                  ReservationRepository reservationRepository,
                                  ShopRepository shopRepository,
                                  ReservationService reservationService) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.shopRepository = shopRepository;
        this.reservationService = reservationService;
    }

    @PostMapping("/products/{id}/reservations")
    public String reserve(@PathVariable Long id,
                           @Valid @ModelAttribute("reservationForm") ReservationForm form,
                           BindingResult bindingResult,
                           Model model,
                           @AuthenticationPrincipal CustomUserDetails principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!bindingResult.hasErrors()
                && (product.getStatus() != ProductStatus.ON_SALE
                    || form.getQuantity() > product.getRemainingQuantity())) {
            bindingResult.rejectValue("quantity", "outOfStock",
                    "指定した数量は予約できません（残り" + product.getRemainingQuantity() + "点）");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("product", ProductDetail.from(product, Instant.now()));
            return "products/detail";
        }

        Reservation reservation = reservationService.reserve(product, principal.getUser(), form.getQuantity());
        return "redirect:/reservations/" + reservation.getId();
    }

    @GetMapping("/reservations/{id}")
    public String confirm(@PathVariable Long id, Model model, @AuthenticationPrincipal CustomUserDetails principal) {
        Reservation reservation = findOwnedByBuyer(id, principal);
        model.addAttribute("reservation", ReservationView.from(reservation));
        return "reservations/confirm";
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancel(@PathVariable Long id,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        CanceledBy actor = resolveActor(reservation, principal.getUser());

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            redirectAttributes.addFlashAttribute("cancelError", "この予約は既に処理済みです");
        } else {
            reservationService.cancel(reservation, actor);
        }

        return actor == CanceledBy.SELLER
                ? "redirect:/seller/dashboard"
                : "redirect:/reservations/" + id;
    }

    private Reservation findOwnedByBuyer(Long id, CustomUserDetails principal) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!reservation.getBuyer().getId().equals(principal.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return reservation;
    }

    private CanceledBy resolveActor(Reservation reservation, User user) {
        if (reservation.getBuyer().getId().equals(user.getId())) {
            return CanceledBy.BUYER;
        }
        Shop shop = shopRepository.findByUserId(user.getId()).orElse(null);
        if (shop != null && reservation.getProduct().getShop().getId().equals(shop.getId())) {
            return CanceledBy.SELLER;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
