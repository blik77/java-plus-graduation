package evm.category.service;

import evm.category.dto.CategoryDto;
import evm.category.dto.NewCategoryDto;
import evm.category.mapper.CategoryMapper;
import evm.category.model.Category;
import evm.category.repository.CategoryRepository;
import evm.event.repository.EventRepository;
import evm.common.exceptions.ConflictException;
import evm.common.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto insertCategory(NewCategoryDto newCategoryDto) {
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория с таким именем " + newCategoryDto.getName() + " уже существует");
        }
        return CategoryMapper.toDto(categoryRepository.save(CategoryMapper.toEntity(newCategoryDto)));
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        if (categoryRepository.existsByNameAndIdNot(categoryDto.getName(), id)) {
            throw new ConflictException(
                    "Категория с таким именем " + categoryDto.getName() + " уже существует"
            );
        }

        Category category = findCategoryOrRaiseException(id);
        category.setName(categoryDto.getName());
        return CategoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryOrRaiseException(id);
        if (eventRepository.existsByCategoryId(id)) {
            throw new ConflictException("Категория \"" + category.getName() + "\" непустая, есть относящиеся к ней события. Удаление невозможно.");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public CategoryDto getCategoryById(Long id) {
        Category category = findCategoryOrRaiseException(id);
        return CategoryMapper.toDto(category);
    }

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("name"));
        return categoryRepository.findAll(pageable).stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());

    }

    private Category findCategoryOrRaiseException(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + categoryId.toString() + " не найдена!"));
    }

}
