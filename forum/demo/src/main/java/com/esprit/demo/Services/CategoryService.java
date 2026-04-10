package com.esprit.demo.Services;

import com.esprit.demo.Dto.CategoryRequest;
import com.esprit.demo.Dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    List<CategoryResponse> getAll();

    CategoryResponse getById(Long id);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);
}
