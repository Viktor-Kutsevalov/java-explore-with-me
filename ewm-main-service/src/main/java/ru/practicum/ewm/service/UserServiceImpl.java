package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.user.NewUserRequest;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(NewUserRequest dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("User with email=" + dto.getEmail() + " already exists");
        }
        User user = UserMapper.toEntity(dto);
        User saved = userRepository.save(user);
        log.debug("Created user: {}", saved);
        return UserMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAll(List<Long> ids, int from, int size) {
        Pageable pageable = buildPageable(from, size);
        List<User> users = (ids == null || ids.isEmpty())
                ? userRepository.findAll(pageable).getContent()
                : userRepository.findAllByIdIn(ids, pageable);
        return users.stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        userRepository.delete(user);
        log.debug("Deleted user id={}", userId);
    }

    private Pageable buildPageable(int from, int size) {
        if (size <= 0) {
            throw new BadRequestException("Size must be positive");
        }
        return PageRequest.of(from / size, size);
    }
}
