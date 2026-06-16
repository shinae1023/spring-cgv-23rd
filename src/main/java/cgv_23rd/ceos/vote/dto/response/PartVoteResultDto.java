package cgv_23rd.ceos.vote.dto.response;

import cgv_23rd.ceos.user.entity.Part;
import lombok.Builder;

@Builder
public record PartVoteResultDto(Long candidateId, String name, Part part, Long voteCount) {
}
