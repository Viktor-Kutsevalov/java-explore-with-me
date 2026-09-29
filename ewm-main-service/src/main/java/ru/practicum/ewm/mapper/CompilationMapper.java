package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.model.Compilation;

import java.util.List;

@UtilityClass
public class CompilationMapper {

    public CompilationDto toDto(Compilation compilation) {
        List<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(EventMapper::toShortDto)
                .toList();

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventDtos)
                .build();
    }
}
