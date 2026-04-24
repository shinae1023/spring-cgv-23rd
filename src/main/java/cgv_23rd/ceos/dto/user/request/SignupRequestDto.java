package cgv_23rd.ceos.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.time.LocalDate;

public record SignupRequestDto(@NotBlank String name, @NotBlank String email,
                               @NotBlank LocalDate birth, @NotBlank String phone,
                               @NotBlank String password) {
}
