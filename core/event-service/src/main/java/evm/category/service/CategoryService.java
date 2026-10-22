package evm.category.service;

import evm.category.dto.CategoryDto;
import evm.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto insertCategory(NewCategoryDto newCategoryDto);

    CategoryDto updateCategory(Long id, CategoryDto categoryDto);

    CategoryDto getCategoryById(Long id);

    void deleteCategory(Long id);

    List<CategoryDto> getCategories(Integer from, Integer size);
}
