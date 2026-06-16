package cgv_23rd.ceos.vote.repository;

import cgv_23rd.ceos.vote.dto.response.TeamVoteResultDto;
import cgv_23rd.ceos.vote.entity.TeamVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeamVoteRepository extends JpaRepository<TeamVote, Long> {

    boolean existsByVoterId(Long voterId);

    @Query("""
            select new cgv_23rd.ceos.vote.dto.response.TeamVoteResultDto(
                tv.candidateTeam,
                count(tv)
            )
            from TeamVote tv
            group by tv.candidateTeam
            order by count(tv) desc, tv.candidateTeam asc
            """)
    List<TeamVoteResultDto> findRankedTeams();
}
