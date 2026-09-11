package com.example.VegeTabell.app.form;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductForm {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @NotNull
    private Long categoryId;

    // Step5申し送り：画像は本アップロードではなくURL入力のみ（overview.mdの申し送り事項に対する暫定対応）
    @Size(max = 500)
    @Pattern(regexp = "^$|^https?://.+", message = "http(s)://から始まるURLを入力してください")
    private String imageUrl;

    @NotNull
    @Min(0)
    private Integer normalPrice;

    @NotNull
    @Min(0)
    private Integer rescuePrice;

    @NotNull
    @Min(1)
    private Integer totalQuantity;

    @NotNull
    @Future
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime expiryAt;
}
