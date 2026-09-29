package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto create(NewCategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name=" + dto.getName() + " already exists");
        }
        Category category = CategoryMapper.toEntity(dto);
        Category saved = categoryRepository.save(category);
        log.debug("Created category: {}", saved);
        return CategoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CategoryDto update(Long catId, CategoryDto dto) {
        Category category = getCategoryOrThrow(catId);
        if (!category.getName().equals(dto.getName()) && categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name=" + dto.getName() + " already exists");
        }
        category.setName(dto.getName());
        Category saved = categoryRepository.save(category);
        log.debug("Updated category: {}", saved);
        return CategoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        Category category = getCategoryOrThrow(catId);
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.delete(category);
        log.debug("Deleted category id={}", catId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAll(int from, int size) {
        Pageable pageable = buildPageable(from, size);
        return categoryRepository.findAll(pageable).stream()
                .map(CategoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getById(Long catId) {
        return CategoryMapper.toDto(getCategoryOrThrow(catId));
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }

    private Pageable buildPageable(int from, int size) {
        if (size <= 0) {
            throw new BadRequestException("Size must be positive");
        }
        return PageRequest.of(from / size, size);
    }
}
