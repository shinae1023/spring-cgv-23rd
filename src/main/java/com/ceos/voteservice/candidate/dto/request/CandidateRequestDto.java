package com.ceos.voteservice.candidate.dto.request;

import com.ceos.voteservice.user.entity.Part;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CandidateRequestDto(
        @NotBlank(message = "후보자 이름은 필수입니다.") String name,
        @NotNull(message = "후보자 파트는 필수입니다.") Part part
) {
}
