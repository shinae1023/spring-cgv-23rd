package com.ceos.voteservice.candidate.dto.response;

import lombok.Builder;

@Builder
public record CandidateResponseDto(Long candidateId, String name, Long voteCount) {

}
