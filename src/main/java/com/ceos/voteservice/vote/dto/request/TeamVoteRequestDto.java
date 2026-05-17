package com.ceos.voteservice.vote.dto.request;

import com.ceos.voteservice.user.entity.Team;
import jakarta.validation.constraints.NotNull;

public record TeamVoteRequestDto(@NotNull(message = "투표할 팀은 필수입니다.") Team team) {
}
