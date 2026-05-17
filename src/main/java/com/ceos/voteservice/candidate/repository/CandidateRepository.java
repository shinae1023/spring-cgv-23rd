package com.ceos.voteservice.candidate.repository;

import com.ceos.voteservice.candidate.entity.Candidate;
import com.ceos.voteservice.candidate.dto.response.CandidateResponseDto;
import com.ceos.voteservice.user.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    @Query("""
            select new com.ceos.voteservice.candidate.dto.response.CandidateResponseDto(
                c.id,
                c.name,
                count(pv)
            )
            from Candidate c
            left join PartVote pv on pv.candidate = c
            where c.part = :part
            group by c.id, c.name
            order by count(pv) desc, c.id asc
            """)
    List<CandidateResponseDto> findAllByPartOrderByVoteCountDesc(@Param("part") Part part);
}
