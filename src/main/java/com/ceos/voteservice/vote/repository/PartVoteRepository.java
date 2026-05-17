package com.ceos.voteservice.vote.repository;

import com.ceos.voteservice.vote.entity.PartVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartVoteRepository extends JpaRepository<PartVote,Long> {

    boolean existsByVoterId(Long voterId);
}
