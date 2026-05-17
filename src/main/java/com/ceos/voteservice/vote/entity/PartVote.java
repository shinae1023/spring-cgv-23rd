package com.ceos.voteservice.vote.entity;

import com.ceos.voteservice.candidate.entity.Candidate;
import com.ceos.voteservice.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
