package com.example.shop.controller;

import com.example.shop.dto.CategoryDto;
import com.example.shop.dto.PageResponse;
import com.example.shop.dto.ProductDto;
import com.example.shop.service.CategoryService;
import com.example.shop.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Catalog (public)")
public class CatalogController {
    private final ProductService productService;
    private final CategoryService categoryService;

    public CatalogController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/products")
    @Operation(summary = "Search products (keyword, category, price range, sort) with server-side pagination",
            description = "sort = newest | price_asc | price_desc | name. size is capped at 50.")
    public PageResponse<ProductDto> search(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) BigDecimal minPrice,
                                           @RequestParam(required = false) BigDecimal maxPrice,
                                           @RequestParam(defaultValue = "newest") String sort,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "12") int size) {
        return productService.search(keyword, categoryId, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/products/{id}")
    public ProductDto get(@PathVariable Long id) {
        return productService.get(id);
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return categoryService.list();
    }
}
