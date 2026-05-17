package com.ceos.voteservice.vote.dto.request;

import jakarta.validation.constraints.NotNull;

public record PartVoteRequestDto(@NotNull(message = "후보자 아이디는 필수입니다.") Long candidateId) {
}
