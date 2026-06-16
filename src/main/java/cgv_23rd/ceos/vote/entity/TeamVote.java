package cgv_23rd.ceos.vote.entity;

import cgv_23rd.ceos.user.entity.Team;
import cgv_23rd.ceos.user.entity.User;
import jakarta.persistence.*;
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
