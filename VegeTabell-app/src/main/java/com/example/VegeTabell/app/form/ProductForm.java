package com.example.VegeTabell.app.form;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

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

    private MultipartFile photo;

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
