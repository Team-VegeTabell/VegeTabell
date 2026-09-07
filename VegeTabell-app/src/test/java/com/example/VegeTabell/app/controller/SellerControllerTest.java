package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.entity.Category;
import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Shop;
import com.example.VegeTabell.app.entity.User;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import com.example.VegeTabell.app.entity.type.UserRole;
import com.example.VegeTabell.app.repository.CategoryRepository;
import com.example.VegeTabell.app.repository.ProductRepository;
import com.example.VegeTabell.app.repository.ReservationRepository;
import com.example.VegeTabell.app.repository.ShopRepository;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SellerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShopRepository shopRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ReservationRepository reservationRepository;

    private Shop shopOwnedBySeller(long shopId, long sellerUserId) {
        User seller = new User();
        seller.setId(sellerUserId);
        seller.setEmail("seller@example.com");
        seller.setRole(UserRole.SELLER);
        seller.setDisplayName("テスト店主");

        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setUser(seller);
        shop.setShopName("大地の恵み 八百屋");
        return shop;
    }

    private UserDetails sellerPrincipal(long sellerUserId) {
        User user = new User();
        user.setId(sellerUserId);
        user.setEmail("seller@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.SELLER);
        user.setDisplayName("テスト店主");
        return new CustomUserDetails(user);
    }

    private Category vegetableCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("野菜");
        category.setIcon("🥦");
        return category;
    }

    @Test
    void getDashboard_showsShopAndProducts() throws Exception {
        Shop shop = shopOwnedBySeller(10L, 1L);
        when(shopRepository.findByUserId(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findByShopIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        when(productRepository.countByShopIdAndCreatedAtGreaterThanEqual(anyLong(), any(Instant.class))).thenReturn(0L);

        mockMvc.perform(get("/seller/dashboard").with(user(sellerPrincipal(1L))))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/dashboard"))
                .andExpect(model().attribute("shop", shop));
    }

    @Test
    void getDashboard_rendersProductCardsWithoutError() throws Exception {
        Shop shop = shopOwnedBySeller(10L, 1L);

        Product onSale = new Product();
        onSale.setId(1L);
        onSale.setShop(shop);
        onSale.setName("新鮮こだわりトマト袋詰め");
        onSale.setNormalPrice(580);
        onSale.setRescuePrice(240);
        onSale.setRemainingQuantity(3);
        onSale.setStatus(ProductStatus.ON_SALE);

        Product soldOut = new Product();
        soldOut.setId(2L);
        soldOut.setShop(shop);
        soldOut.setName("ほうれん草バラ売り");
        soldOut.setImageUrl("https://example.com/image.jpg");
        soldOut.setNormalPrice(200);
        soldOut.setRescuePrice(80);
        soldOut.setRemainingQuantity(0);
        soldOut.setStatus(ProductStatus.SOLD_OUT);

        when(shopRepository.findByUserId(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findByShopIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(onSale, soldOut));
        when(productRepository.countByShopIdAndCreatedAtGreaterThanEqual(anyLong(), any(Instant.class))).thenReturn(2L);

        mockMvc.perform(get("/seller/dashboard").with(user(sellerPrincipal(1L))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("新鮮こだわりトマト袋詰め"),
                        org.hamcrest.Matchers.containsString("完売にする"),
                        org.hamcrest.Matchers.containsString("完売しました"),
                        org.hamcrest.Matchers.containsString("background-image:url(https://example.com/image.jpg)")
                )));
    }

    @Test
    void getNewProductForm_showsFormWithCategories() throws Exception {
        List<Category> categories = List.of(vegetableCategory());
        when(categoryRepository.findAllByOrderById()).thenReturn(categories);

        mockMvc.perform(get("/seller/products/new").with(user(sellerPrincipal(1L))))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/product-form"))
                .andExpect(model().attributeExists("productForm"))
                .andExpect(model().attribute("categories", categories));
    }

    @Test
    void postProduct_happyPath_initializesRemainingQuantityAndRedirects() throws Exception {
        Shop shop = shopOwnedBySeller(10L, 1L);
        when(shopRepository.findByUserId(1L)).thenReturn(Optional.of(shop));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(vegetableCategory()));

        String futureExpiry = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES).toString();

        mockMvc.perform(post("/seller/products")
                        .with(user(sellerPrincipal(1L)))
                        .param("name", "ほうれん草バラ売り")
                        .param("categoryId", "1")
                        .param("normalPrice", "200")
                        .param("rescuePrice", "80")
                        .param("totalQuantity", "3")
                        .param("expiryAt", futureExpiry)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/seller/products"));

        verify(productRepository).save(org.mockito.ArgumentMatchers.argThat(product ->
                product.getRemainingQuantity().equals(3)
                        && product.getTotalQuantity().equals(3)
                        && product.getStatus() == ProductStatus.ON_SALE
                        && product.getShop().equals(shop)));
    }

    @Test
    void postProduct_rescuePriceHigherThanNormalPrice_reRendersWithFieldError() throws Exception {
        when(categoryRepository.findAllByOrderById()).thenReturn(List.of(vegetableCategory()));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(vegetableCategory()));

        String futureExpiry = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES).toString();

        mockMvc.perform(post("/seller/products")
                        .with(user(sellerPrincipal(1L)))
                        .param("name", "ほうれん草バラ売り")
                        .param("categoryId", "1")
                        .param("normalPrice", "80")
                        .param("rescuePrice", "200")
                        .param("totalQuantity", "3")
                        .param("expiryAt", futureExpiry)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "rescuePrice"));

        verify(productRepository, never()).save(any());
    }

    @Test
    void postProduct_invalidImageUrl_reRendersWithFieldError() throws Exception {
        when(categoryRepository.findAllByOrderById()).thenReturn(List.of(vegetableCategory()));

        String futureExpiry = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES).toString();

        mockMvc.perform(post("/seller/products")
                        .with(user(sellerPrincipal(1L)))
                        .param("name", "ほうれん草バラ売り")
                        .param("categoryId", "1")
                        .param("imageUrl", "not-a-url")
                        .param("normalPrice", "200")
                        .param("rescuePrice", "80")
                        .param("totalQuantity", "3")
                        .param("expiryAt", futureExpiry)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "imageUrl"));

        verify(productRepository, never()).save(any());
    }

    @Test
    void postProduct_pastExpiry_reRendersWithFieldError() throws Exception {
        when(categoryRepository.findAllByOrderById()).thenReturn(List.of(vegetableCategory()));

        String pastExpiry = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MINUTES).toString();

        mockMvc.perform(post("/seller/products")
                        .with(user(sellerPrincipal(1L)))
                        .param("name", "ほうれん草バラ売り")
                        .param("categoryId", "1")
                        .param("normalPrice", "200")
                        .param("rescuePrice", "80")
                        .param("totalQuantity", "3")
                        .param("expiryAt", pastExpiry)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "expiryAt"));

        verify(productRepository, never()).save(any());
    }

    @Test
    void postSoldOut_ownProduct_updatesStatusAndRedirects() throws Exception {
        Shop shop = shopOwnedBySeller(10L, 1L);
        Product product = new Product();
        product.setId(5L);
        product.setShop(shop);
        product.setStatus(ProductStatus.ON_SALE);

        when(shopRepository.findByUserId(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        mockMvc.perform(post("/seller/products/{id}/sold-out", 5L)
                        .with(user(sellerPrincipal(1L)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/seller/products"));

        verify(productRepository).save(product);
    }

    @Test
    void postSoldOut_otherShopsProduct_isForbidden() throws Exception {
        Shop ownShop = shopOwnedBySeller(10L, 1L);
        Shop otherShop = shopOwnedBySeller(20L, 2L);
        Product product = new Product();
        product.setId(5L);
        product.setShop(otherShop);
        product.setStatus(ProductStatus.ON_SALE);

        when(shopRepository.findByUserId(1L)).thenReturn(Optional.of(ownShop));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        mockMvc.perform(post("/seller/products/{id}/sold-out", 5L)
                        .with(user(sellerPrincipal(1L)))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(productRepository, never()).save(any());
    }

    @Test
    void postDelete_noReservations_deletesAndRedirects() throws Exception {
        Shop shop = shopOwnedBySeller(10L, 1L);
        Product product = new Product();
        product.setId(5L);
        product.setShop(shop);

        when(shopRepository.findByUserId(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(reservationRepository.existsByProductId(5L)).thenReturn(false);

        mockMvc.perform(post("/seller/products/{id}/delete", 5L)
                        .with(user(sellerPrincipal(1L)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/seller/products"));

        verify(productRepository).delete(product);
    }

    @Test
    void postDelete_hasReservations_doesNotDeleteAndRedirectsWithError() throws Exception {
        Shop shop = shopOwnedBySeller(10L, 1L);
        Product product = new Product();
        product.setId(5L);
        product.setShop(shop);

        when(shopRepository.findByUserId(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(reservationRepository.existsByProductId(5L)).thenReturn(true);

        mockMvc.perform(post("/seller/products/{id}/delete", 5L)
                        .with(user(sellerPrincipal(1L)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/seller/products"));

        verify(productRepository, never()).delete(any());
    }

    @Test
    void postProduct_withoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/seller/products")
                        .with(user(sellerPrincipal(1L)))
                        .param("name", "ほうれん草バラ売り")
                        .param("categoryId", "1")
                        .param("normalPrice", "200")
                        .param("rescuePrice", "80")
                        .param("totalQuantity", "3")
                        .param("expiryAt", LocalDateTime.now().plusDays(1).toString()))
                .andExpect(status().isForbidden());
    }
}
