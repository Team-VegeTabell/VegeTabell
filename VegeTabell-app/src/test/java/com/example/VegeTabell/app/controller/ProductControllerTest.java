package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.entity.Category;
import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Shop;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.repository.CategoryRepository;
import com.example.VegeTabell.app.repository.ProductRepository;
import com.example.VegeTabell.app.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    private UserDetails buyerPrincipal() {
        User user = new User();
        user.setId(1L);
        user.setEmail("buyer@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.BUYER);
        user.setDisplayName("テスト買い手");
        return new CustomUserDetails(user);
    }

    private Category vegetableCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("野菜");
        category.setIcon("🥦");
        return category;
    }

    private Product onSaleProduct() {
        Shop shop = new Shop();
        shop.setId(10L);
        shop.setShopName("大地の恵み 八百屋");
        shop.setAddress("東京都世田谷区1-1-1");

        Product product = new Product();
        product.setId(100L);
        product.setShop(shop);
        product.setCategory(vegetableCategory());
        product.setName("新鮮こだわりトマト袋詰め");
        product.setNormalPrice(580);
        product.setRescuePrice(240);
        product.setTotalQuantity(3);
        product.setRemainingQuantity(3);
        product.setExpiryAt(Instant.now().plus(3, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES));
        product.setStatus(ProductStatus.ON_SALE);
        return product;
    }

    @Test
    void root_redirectsToProducts() throws Exception {
        mockMvc.perform(get("/").with(user(buyerPrincipal())))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/products"));
    }

    @Test
    void list_showsOnSaleProductsWithComputedDiscount() throws Exception {
        when(productRepository.findAvailableForBuyer(eq(ProductStatus.ON_SALE), isNull(), isNull(), isNull()))
                .thenReturn(List.of(onSaleProduct()));
        when(categoryRepository.findAllByOrderById()).thenReturn(List.of(vegetableCategory()));

        mockMvc.perform(get("/products").with(user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(content().string(containsString("新鮮こだわりトマト袋詰め")))
                .andExpect(content().string(containsString("59%OFF")))
                .andExpect(content().string(containsString("あと3時間")));
    }

    @Test
    void list_blankKeywordAndArea_passedAsNullToRepository() throws Exception {
        when(productRepository.findAvailableForBuyer(any(), any(), any(), any())).thenReturn(List.of());
        when(categoryRepository.findAllByOrderById()).thenReturn(List.of());

        mockMvc.perform(get("/products").param("keyword", "").param("area", "").with(user(buyerPrincipal())))
                .andExpect(status().isOk());

        verify(productRepository).findAvailableForBuyer(eq(ProductStatus.ON_SALE), isNull(), isNull(), isNull());
    }

    @Test
    void list_categoryFilter_passedToRepository() throws Exception {
        when(productRepository.findAvailableForBuyer(any(), any(), any(), any())).thenReturn(List.of());
        when(categoryRepository.findAllByOrderById()).thenReturn(List.of(vegetableCategory()));

        mockMvc.perform(get("/products").param("category", "1").with(user(buyerPrincipal())))
                .andExpect(status().isOk());

        verify(productRepository).findAvailableForBuyer(eq(ProductStatus.ON_SALE), eq(1L), isNull(), isNull());
    }

    @Test
    void detail_onSaleProduct_showsReserveButton() throws Exception {
        when(productRepository.findById(100L)).thenReturn(Optional.of(onSaleProduct()));

        mockMvc.perform(get("/products/{id}", 100L).with(user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("products/detail"))
                .andExpect(content().string(containsString("受け取りを予約する")))
                .andExpect(content().string(containsString("お早めに")));
    }

    @Test
    void detail_soldOutProduct_disablesReserveButton() throws Exception {
        Product product = onSaleProduct();
        product.setStatus(ProductStatus.SOLD_OUT);
        product.setRemainingQuantity(0);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        mockMvc.perform(get("/products/{id}", 100L).with(user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("完売しました")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("受け取りを予約する"))));
    }

    @Test
    void detail_notFound_returns404() throws Exception {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/{id}", 999L).with(user(buyerPrincipal())))
                .andExpect(status().isNotFound());
    }
}
