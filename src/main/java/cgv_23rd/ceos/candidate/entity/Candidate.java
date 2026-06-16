package cgv_23rd.ceos.candidate.entity;

import cgv_23rd.ceos.user.entity.Part;
import cgv_23rd.ceos.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "candidates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Part part;

    @Builder
    private Candidate(String name, Part part) {
        this.name = name;
        this.part = part;
    }

    public void validateVotableBy(User voter) {
        if (voter.getPart() != part) {
            throw new IllegalArgumentException("본인 파트의 후보자에게만 투표할 수 있습니다.");
        }
    }
}
