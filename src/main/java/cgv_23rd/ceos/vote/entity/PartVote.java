package cgv_23rd.ceos.vote.entity;

import cgv_23rd.ceos.candidate.entity.Candidate;
import cgv_23rd.ceos.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "part_vote",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "part_voter_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartVote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_voter_id", nullable = false)
    private User voter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_candidate_id", nullable = false)
    private Candidate candidate;

    private PartVote(User voter, Candidate candidate) {
        this.voter = voter;
        this.candidate = candidate;
    }

    public static PartVote create(User voter, Candidate candidate) {
        candidate.validateVotableBy(voter);
        return new PartVote(voter, candidate);
    }
}
