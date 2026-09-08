package com.example.VegeTabell.app.form;

import com.example.VegeTabell.app.entity.Shop;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ShopForm {

    @NotBlank
    @Size(max = 100)
    private String shopName;

    @NotBlank
    @Size(max = 255)
    private String address;

    @NotNull
    private BigDecimal latitude;

    @NotNull
    private BigDecimal longitude;

    private String pickupNote;

    public static ShopForm from(Shop shop) {
        ShopForm form = new ShopForm();
        form.setShopName(shop.getShopName());
        form.setAddress(shop.getAddress());
        form.setLatitude(shop.getLatitude());
        form.setLongitude(shop.getLongitude());
        form.setPickupNote(shop.getPickupNote());
        return form;
    }
}
