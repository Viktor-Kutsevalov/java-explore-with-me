package ru.practicum.ewm.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {

    @NotEmpty(message = "requestIds must not be empty")
    private List<Long> requestIds;

    @NotNull(message = "status must not be null")
    private Status status;

    public enum Status {
        CONFIRMED,
        REJECTED
    }
}
