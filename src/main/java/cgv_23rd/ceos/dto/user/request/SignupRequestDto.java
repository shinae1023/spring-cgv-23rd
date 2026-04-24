package cgv_23rd.ceos.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SignupRequestDto(@NotBlank String name, @NotBlank String email,
                               @NotNull LocalDate birth, @NotBlank String phone,
                               @NotBlank String password) {
}
