package com.ceos.voteservice.vote.dto.response;

import com.ceos.voteservice.user.entity.Team;
import lombok.Builder;

@Builder
public record TeamVoteResultDto(Team team, Long voteCount) {
}
