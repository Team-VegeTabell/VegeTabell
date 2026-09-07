package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.entity.Category;
import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.Shop;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import com.example.VegeTabell.app.form.ProductForm;
import com.example.VegeTabell.app.repository.CategoryRepository;
import com.example.VegeTabell.app.repository.ProductRepository;
import com.example.VegeTabell.app.repository.ReservationRepository;
import com.example.VegeTabell.app.repository.ShopRepository;
import com.example.VegeTabell.app.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequestMapping("/seller")
public class SellerController {

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReservationRepository reservationRepository;

    public SellerController(ShopRepository shopRepository,
                             ProductRepository productRepository,
                             CategoryRepository categoryRepository,
                             ReservationRepository reservationRepository) {
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal CustomUserDetails principal) {
        Shop shop = currentShop(principal);
        Instant startOfToday = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        model.addAttribute("shop", shop);
        model.addAttribute("products", productRepository.findByShopIdOrderByCreatedAtDesc(shop.getId()));
        model.addAttribute("todayListedCount",
                productRepository.countByShopIdAndCreatedAtGreaterThanEqual(shop.getId(), startOfToday));
        return "seller/dashboard";
    }

    @GetMapping("/products")
    public String products(Model model, @AuthenticationPrincipal CustomUserDetails principal) {
        Shop shop = currentShop(principal);
        model.addAttribute("products", productRepository.findByShopIdOrderByCreatedAtDesc(shop.getId()));
        return "seller/products";
    }

    @GetMapping("/products/new")
    public String newProductForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        model.addAttribute("categories", categoryRepository.findAllByOrderById());
        return "seller/product-form";
    }

    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute("productForm") ProductForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        if (!bindingResult.hasErrors()
                && form.getRescuePrice() > form.getNormalPrice()) {
            bindingResult.rejectValue("rescuePrice", "tooHigh", "レスキュー価格は通常価格以下にしてください");
        }

        Category category = null;
        if (!bindingResult.hasFieldErrors("categoryId")) {
            category = categoryRepository.findById(form.getCategoryId()).orElse(null);
            if (category == null) {
                bindingResult.rejectValue("categoryId", "invalid", "カテゴリを選択してください");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAllByOrderById());
            return "seller/product-form";
        }

        Shop shop = currentShop(principal);

        Product product = new Product();
        product.setShop(shop);
        product.setCategory(category);
        product.setName(form.getName());
        product.setImageUrl(StringUtils.hasText(form.getImageUrl()) ? form.getImageUrl() : null);
        product.setNormalPrice(form.getNormalPrice());
        product.setRescuePrice(form.getRescuePrice());
        product.setTotalQuantity(form.getTotalQuantity());
        product.setRemainingQuantity(form.getTotalQuantity());
        product.setExpiryAt(form.getExpiryAt().atZone(ZoneId.systemDefault()).toInstant());
        product.setStatus(ProductStatus.ON_SALE);
        productRepository.save(product);

        return "redirect:/seller/products";
    }

    @PostMapping("/products/{id}/sold-out")
    public String markSoldOut(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        Product product = requireOwnedProduct(id, principal);
        product.setStatus(ProductStatus.SOLD_OUT);
        productRepository.save(product);
        return "redirect:/seller/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        Product product = requireOwnedProduct(id, principal);

        if (reservationRepository.existsByProductId(id)) {
            redirectAttributes.addFlashAttribute("deleteError", "予約が入っている商品は削除できません");
            return "redirect:/seller/products";
        }

        productRepository.delete(product);
        return "redirect:/seller/products";
    }

    private Shop currentShop(CustomUserDetails principal) {
        return shopRepository.findByUserId(principal.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "店舗情報が見つかりません"));
    }

    private Product requireOwnedProduct(Long id, CustomUserDetails principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Shop shop = currentShop(principal);
        if (!product.getShop().getId().equals(shop.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return product;
    }
}
