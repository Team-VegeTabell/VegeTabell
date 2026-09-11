package com.example.VegeTabell.app.form;

import com.example.VegeTabell.app.entity.type.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupForm {

    @NotNull
    private UserRole role = UserRole.BUYER;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    // 最低8文字は要件定義に明記が無いための暫定値。チームで要確認。
    @NotBlank
    @Size(min = 8, max = 255)
    private String password;

    // 買い手のみ必須（Controllerで手動バリデーション）。売り手は店舗名をdisplayNameとして使うため未入力でよい。
    @Size(max = 50)
    private String displayName;

    // --- 以下、売り手のみ必須（Controllerで手動バリデーション） ---
    @Size(max = 100)
    private String shopName;

    @Size(max = 255)
    private String address;

    private String pickupNote;
}
