package cgv_23rd.ceos.vote.service;

import cgv_23rd.ceos.user.entity.Team;
import cgv_23rd.ceos.user.entity.User;
import cgv_23rd.ceos.user.repository.UserRepository;
import cgv_23rd.ceos.vote.dto.request.TeamVoteRequestDto;
import cgv_23rd.ceos.vote.dto.response.TeamVoteResultDto;
import cgv_23rd.ceos.vote.entity.TeamVote;
import cgv_23rd.ceos.vote.repository.TeamVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TeamVoteService {

    private final UserRepository userRepository;
    private final TeamVoteRepository teamVoteRepository;

    @Transactional
    public String createTeamVote(Long userId, TeamVoteRequestDto requestDto) {
        User voter = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        if (teamVoteRepository.existsByVoterId(userId)) {
            throw new IllegalArgumentException("팀 투표는 1회만 가능합니다.");
        }

        teamVoteRepository.save(TeamVote.create(voter, requestDto.team()));
        return "팀 투표가 완료되었습니다.";
    }

    @Transactional(readOnly = true)
    public List<TeamVoteResultDto> getTeamVoteResults() {
        List<TeamVoteResultDto> rankedTeams = teamVoteRepository.findRankedTeams();
        Map<Team, TeamVoteResultDto> rankedTeamMap = rankedTeams.stream()
                .collect(java.util.stream.Collectors.toMap(TeamVoteResultDto::team, Function.identity()));
        List<Team> unvotedTeams = Arrays.stream(Team.values())
                .filter(team -> !rankedTeamMap.containsKey(team))
                .toList();

        return java.util.stream.Stream.concat(
                        rankedTeams.stream(),
                        unvotedTeams.stream().map(team -> TeamVoteResultDto.builder()
                                .team(team)
                                .voteCount(0L)
                                .build())
                )
                .toList();
    }
}
