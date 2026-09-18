package ru.practicum.ewm.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewUserRequest {

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    @Size(min = 6, max = 254, message = "email must be between 6 and 254 characters")
    private String email;

    @NotBlank(message = "name must not be blank")
    @Size(min = 2, max = 250, message = "name must be between 2 and 250 characters")
    private String name;
}
