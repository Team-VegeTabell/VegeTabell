package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.form.SignupForm;
import com.example.VegeTabell.app.service.SignupService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final SignupService signupService;

    public AuthController(SignupService signupService) {
        this.signupService = signupService;
    }

    @GetMapping("/login")
    public String login() {
        // エラー/ロール不一致/ログアウト/登録完了メッセージは
        // クエリパラメータ(?error, ?error=role, ?logout, ?registered)を
        // テンプレート側でth:ifにより直接判定するため、Model詰め込み不要。
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signupForm") SignupForm form,
                          BindingResult bindingResult) {

        if (form.getRole() == UserRole.SELLER) {
            validateSellerFields(form, bindingResult);
        }

        if (!bindingResult.hasErrors() && signupService.emailExists(form.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "このメールアドレスは既に登録されています");
        }

        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        signupService.register(form);

        return "redirect:/login?registered";
    }

    private void validateSellerFields(SignupForm form, BindingResult bindingResult) {
        if (!StringUtils.hasText(form.getShopName())) {
            bindingResult.rejectValue("shopName", "required", "店舗名を入力してください");
        }
        if (!StringUtils.hasText(form.getAddress())) {
            bindingResult.rejectValue("address", "required", "住所を入力してください");
        }
        if (!StringUtils.hasText(form.getPickupNote())) {
            bindingResult.rejectValue("pickupNote", "required", "受け取り場所の説明を入力してください");
        }
        if (form.getLatitude() == null) {
            bindingResult.rejectValue("latitude", "required", "緯度を入力してください");
        }
        if (form.getLongitude() == null) {
            bindingResult.rejectValue("longitude", "required", "経度を入力してください");
        }
    }
}
