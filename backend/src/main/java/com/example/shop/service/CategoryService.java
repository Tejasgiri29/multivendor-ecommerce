package com.example.shop.service;

import com.example.shop.dto.CategoryDto;
import com.example.shop.dto.CategoryRequest;
import com.example.shop.entity.Category;
import com.example.shop.exception.ConflictException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.CategoryRepository;
import com.example.shop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(DtoMapper::toDto).toList();
    }

    @Transactional
    public CategoryDto create(CategoryRequest req) {
        if (categoryRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw new ConflictException("Category name already exists");
        }
        Category c = Category.builder().name(req.name().trim()).description(req.description()).build();
        return DtoMapper.toDto(categoryRepository.save(c));
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest req) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        String name = req.name().trim();
        if (!c.getName().equalsIgnoreCase(name) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category name already exists");
        }
        c.setName(name);
        c.setDescription(req.description());
        return DtoMapper.toDto(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category still has products and cannot be deleted");
        }
        categoryRepository.deleteById(id);
    }
}
