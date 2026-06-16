package cgv_23rd.ceos.candidate.dto.response;

import lombok.Builder;

@Builder
public record CandidateResponseDto(Long candidateId, String name, Long voteCount) {

}
