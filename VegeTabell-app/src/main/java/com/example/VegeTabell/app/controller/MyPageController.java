package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.dto.MyPageReservationView;
import com.example.VegeTabell.app.repository.ReservationRepository;
import com.example.VegeTabell.app.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MyPageController {

    private final ReservationRepository reservationRepository;

    public MyPageController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/mypage")
    public String mypage(Model model, @AuthenticationPrincipal CustomUserDetails principal) {
        model.addAttribute("reservations", reservationRepository
                .findByBuyerIdOrderByReservedAtDesc(principal.getUser().getId())
                .stream()
                .map(MyPageReservationView::from)
                .toList());
        return "mypage";
    }
}
