package evm.main.category.mapper;


import evm.main.category.dto.CategoryDto;
import evm.main.category.dto.NewCategoryDto;
import evm.main.category.model.Category;

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
