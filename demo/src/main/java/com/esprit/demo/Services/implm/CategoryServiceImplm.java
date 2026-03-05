package com.esprit.demo.Services.implm;

import com.esprit.demo.Dto.CategoryRequest;
import com.esprit.demo.Dto.CategoryResponse;
import com.esprit.demo.Exceptions.ResourceNotFoundException;
import com.esprit.demo.Models.Category;
import com.esprit.demo.Repositories.CategoryRepository;
import com.esprit.demo.Services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImplm implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public CategoryResponse create(CategoryRequest request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return toResponse(categoryRepository.save(category));
    }



    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return toResponse(categoryRepository.save(category));
    }

    @Override
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Catégorie introuvable avec l'id : " + id);
        }
        categoryRepository.deleteById(id);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'id : " + id));
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
