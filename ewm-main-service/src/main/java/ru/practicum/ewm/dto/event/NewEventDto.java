package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {

    @NotBlank(message = "annotation must not be blank")
    @Size(min = 20, max = 2000, message = "annotation must be between 20 and 2000 characters")
    private String annotation;

    @NotNull(message = "category must not be null")
    private Long category;

    @NotBlank(message = "description must not be blank")
    @Size(min = 20, max = 7000, message = "description must be between 20 and 7000 characters")
    private String description;

    @NotNull(message = "eventDate must not be null")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull(message = "location must not be null")
    @Valid
    private LocationDto location;

    private Boolean paid;

    @PositiveOrZero(message = "participantLimit must be >= 0")
    private Integer participantLimit;

    private Boolean requestModeration;

    @NotBlank(message = "title must not be blank")
    @Size(min = 3, max = 120, message = "title must be between 3 and 120 characters")
    private String title;
}
