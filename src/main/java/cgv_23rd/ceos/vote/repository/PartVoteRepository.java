package cgv_23rd.ceos.vote.repository;

import cgv_23rd.ceos.vote.entity.PartVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartVoteRepository extends JpaRepository<PartVote,Long> {

    boolean existsByVoterId(Long voterId);
}
