package com.example.VegeTabell.app.repository;

import com.example.VegeTabell.app.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("local")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findAllReturnsSeededCategories() {
        List<Category> categories = categoryRepository.findAll();

        assertEquals(4, categories.size());

        Set<String> names = categories.stream()
                .map(Category::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("野菜", "果物", "パン", "惣菜"), names);
    }
}
