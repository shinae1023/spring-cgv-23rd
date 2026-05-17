package com.ceos.voteservice.vote.dto.response;

import com.ceos.voteservice.user.entity.Part;
import lombok.Builder;

@Builder
public record PartVoteResultDto(Long candidateId, String name, Part part, Long voteCount) {
}
