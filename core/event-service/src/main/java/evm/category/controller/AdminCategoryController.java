package evm.category.controller;

import evm.category.dto.CategoryDto;
import evm.category.dto.NewCategoryDto;
import evm.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {
    private final CategoryService service;

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public CategoryDto create(@RequestBody @Valid NewCategoryDto request) {
        log.info("Admin, запрос создания категории: {}", request);
        CategoryDto result = service.insertCategory(request);
        log.info("Admin, результат создания категории: {}", result);
        return result;
    }

    @PatchMapping("/{catId}")
    public CategoryDto update(@PathVariable Long catId, @RequestBody @Valid CategoryDto categoryDto) {
        log.info("Admin, запрос обновления категории {}", categoryDto);
        CategoryDto result = service.updateCategory(catId, categoryDto);
        log.info("Admin, результат обновления категории {}", result);
        return result;
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long catId) {
        log.info("Admin, запрос удаления категории {}", catId);
        service.deleteCategory(catId);
        log.info("Admin, результат удаления категории {}", catId);
    }
}
