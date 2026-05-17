package com.ceos.voteservice.vote.entity;

import com.ceos.voteservice.user.entity.Team;
import com.ceos.voteservice.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "team_vote",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "team_voter_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_voter_id", nullable = false)
    private User voter;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(nullable = false)
    private Team candidateTeam;

    private TeamVote(User voter, Team candidateTeam) {
        this.voter = voter;
        this.candidateTeam = candidateTeam;
    }

    public static TeamVote create(User voter, Team candidateTeam) {
        voter.validateCanVoteFor(candidateTeam);
        return new TeamVote(voter, candidateTeam);
    }
}
