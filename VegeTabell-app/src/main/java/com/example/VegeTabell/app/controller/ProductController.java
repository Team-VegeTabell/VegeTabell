package com.example.VegeTabell.app.controller;

import com.example.VegeTabell.app.dto.ProductDetail;
import com.example.VegeTabell.app.dto.ProductSummary;
import com.example.VegeTabell.app.entity.Product;
import com.example.VegeTabell.app.entity.type.ProductStatus;
import com.example.VegeTabell.app.form.ReservationForm;
import com.example.VegeTabell.app.repository.CategoryRepository;
import com.example.VegeTabell.app.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Controller
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductController(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/")
    public String top() {
        return "redirect:/products";
    }

    @GetMapping("/products")
    public String list(@RequestParam(required = false) Long category,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String area,
                        Model model) {
        Instant now = Instant.now();
        List<ProductSummary> products = productRepository
                .findAvailableForBuyer(ProductStatus.ON_SALE, category,
                        likePattern(keyword), likePattern(area))
                .stream()
                .map(product -> ProductSummary.from(product, now))
                .toList();

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAllByOrderById());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("area", area);
        return "products/list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("product", ProductDetail.from(product, Instant.now()));
        model.addAttribute("reservationForm", new ReservationForm());
        return "products/detail";
    }

    private String likePattern(String value) {
        return StringUtils.hasText(value) ? "%" + value.toLowerCase() + "%" : null;
    }
}
