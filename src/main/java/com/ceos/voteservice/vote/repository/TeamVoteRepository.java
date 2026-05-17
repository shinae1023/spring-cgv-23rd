package com.ceos.voteservice.vote.repository;

import com.ceos.voteservice.vote.entity.TeamVote;
import com.ceos.voteservice.user.entity.Team;
import com.ceos.voteservice.vote.dto.response.TeamVoteResultDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeamVoteRepository extends JpaRepository<TeamVote, Long> {

    boolean existsByVoterId(Long voterId);

    @Query("""
            select new com.ceos.voteservice.vote.dto.response.TeamVoteResultDto(
                tv.candidateTeam,
                count(tv)
            )
            from TeamVote tv
            group by tv.candidateTeam
            order by count(tv) desc, tv.candidateTeam asc
            """)
    List<TeamVoteResultDto> findRankedTeams();
}
