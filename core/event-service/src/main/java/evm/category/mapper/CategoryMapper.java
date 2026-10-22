package evm.category.mapper;


import evm.category.dto.CategoryDto;
import evm.category.dto.NewCategoryDto;
import evm.category.model.Category;

public class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category toEntity(NewCategoryDto dto) {
        return Category.builder()
                .name(dto.getName())
                .build();
    }

    public static CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
