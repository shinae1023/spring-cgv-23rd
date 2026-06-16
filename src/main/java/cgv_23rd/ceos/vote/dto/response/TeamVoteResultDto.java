package cgv_23rd.ceos.vote.dto.response;

import cgv_23rd.ceos.user.entity.Team;
import lombok.Builder;

@Builder
public record TeamVoteResultDto(Team team, Long voteCount) {
}
