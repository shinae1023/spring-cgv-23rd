package cgv_23rd.ceos.vote.dto.request;

import cgv_23rd.ceos.user.entity.Team;
import jakarta.validation.constraints.NotNull;

public record TeamVoteRequestDto(@NotNull(message = "투표할 팀은 필수입니다.") Team team) {
}
