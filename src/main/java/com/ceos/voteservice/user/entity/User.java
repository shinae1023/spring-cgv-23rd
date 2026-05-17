package com.ceos.voteservice.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Part part;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Builder
    private User(String loginId, String password, String email, String name, Part part, Team team, UserRole role) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.name = name;
        this.part = part;
        this.team = team;
        this.role = role == null ? UserRole.USER : role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    public boolean isSamePartWith(User other) {
        return part == other.part;
    }

    public boolean isSameTeamWith(Team otherTeam) {
        return team == otherTeam;
    }

    public void validateCanVoteFor(Team targetTeam) {
        if (isSameTeamWith(targetTeam)) {
            throw new IllegalArgumentException("본인 팀에는 투표할 수 없습니다.");
        }
    }
}
