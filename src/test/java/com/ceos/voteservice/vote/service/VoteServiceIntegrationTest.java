package com.ceos.voteservice.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ceos.voteservice.candidate.dto.response.CandidateResponseDto;
import com.ceos.voteservice.candidate.entity.Candidate;
import com.ceos.voteservice.candidate.repository.CandidateRepository;
import com.ceos.voteservice.user.entity.Part;
import com.ceos.voteservice.user.entity.Team;
import com.ceos.voteservice.user.entity.User;
import com.ceos.voteservice.user.repository.UserRepository;
import com.ceos.voteservice.vote.dto.request.PartVoteRequestDto;
import com.ceos.voteservice.vote.dto.request.TeamVoteRequestDto;
import com.ceos.voteservice.vote.dto.response.TeamVoteResultDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "security.jwt.secret=test-secret-key-for-vote-service")
@Transactional
class VoteServiceIntegrationTest {

    @Autowired
    private PartVoteService partVoteService;

    @Autowired
    private TeamVoteService teamVoteService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Test
    void createPartVote_allowsVoteForSamePartCandidateOnce() {
        User voter = saveUser("backend1", "backend1@example.com", Part.BACKEND, Team.IPX);
        Candidate candidate = saveCandidate("백엔드 후보", Part.BACKEND);

        String response = partVoteService.createPartVote(voter.getId(), new PartVoteRequestDto(candidate.getId()));

        assertThat(response).isEqualTo("파트장 투표가 완료되었습니다.");
        List<CandidateResponseDto> results = partVoteService.getPartVoteResults(Part.BACKEND);
        assertThat(results).extracting(CandidateResponseDto::voteCount).containsExactly(1L);
    }

    @Test
    void createPartVote_rejectsDuplicateVoteBySameUser() {
        User voter = saveUser("backend1", "backend1@example.com", Part.BACKEND, Team.IPX);
        Candidate candidate = saveCandidate("백엔드 후보", Part.BACKEND);
        partVoteService.createPartVote(voter.getId(), new PartVoteRequestDto(candidate.getId()));

        assertThatThrownBy(() -> partVoteService.createPartVote(voter.getId(), new PartVoteRequestDto(candidate.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파트장 투표는 1회만 가능합니다.");
    }

    @Test
    void createPartVote_rejectsCandidateFromDifferentPart() {
        User voter = saveUser("frontend1", "frontend1@example.com", Part.FRONTEND, Team.IPX);
        Candidate candidate = saveCandidate("백엔드 후보", Part.BACKEND);

        assertThatThrownBy(() -> partVoteService.createPartVote(voter.getId(), new PartVoteRequestDto(candidate.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인 파트의 후보자에게만 투표할 수 있습니다.");
    }

    @Test
    void createTeamVote_rejectsOwnTeam() {
        User voter = saveUser("backend1", "backend1@example.com", Part.BACKEND, Team.IPX);

        assertThatThrownBy(() -> teamVoteService.createTeamVote(voter.getId(), new TeamVoteRequestDto(Team.IPX)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인 팀에는 투표할 수 없습니다.");
    }

    @Test
    void getTeamVoteResults_includesTeamsWithoutVotes() {
        User voter = saveUser("backend1", "backend1@example.com", Part.BACKEND, Team.IPX);
        teamVoteService.createTeamVote(voter.getId(), new TeamVoteRequestDto(Team.JOBDRI));

        List<TeamVoteResultDto> results = teamVoteService.getTeamVoteResults();

        assertThat(results).hasSize(Team.values().length);
        assertThat(results.getFirst().team()).isEqualTo(Team.JOBDRI);
        assertThat(results.getFirst().voteCount()).isEqualTo(1L);
        assertThat(results).filteredOn(result -> result.team() == Team.IPX)
                .singleElement()
                .extracting(TeamVoteResultDto::voteCount)
                .isEqualTo(0L);
    }

    private User saveUser(String loginId, String email, Part part, Team team) {
        return userRepository.save(User.builder()
                .loginId(loginId)
                .password("encoded-password")
                .email(email)
                .name(loginId)
                .part(part)
                .team(team)
                .build());
    }

    private Candidate saveCandidate(String name, Part part) {
        return candidateRepository.save(Candidate.builder()
                .name(name)
                .part(part)
                .build());
    }
}
