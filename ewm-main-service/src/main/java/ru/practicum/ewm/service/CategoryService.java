package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto create(NewCategoryDto dto);

    CategoryDto update(Long catId, CategoryDto dto);

    void delete(Long catId);

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long catId);
}
