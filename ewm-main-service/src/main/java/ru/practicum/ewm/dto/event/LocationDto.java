package ru.practicum.ewm.dto.event;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    @NotNull(message = "lat must not be null")
    private Float lat;

    @NotNull(message = "lon must not be null")
    private Float lon;
}
