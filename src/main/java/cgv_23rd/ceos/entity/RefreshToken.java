package cgv_23rd.ceos.entity;

import cgv_23rd.ceos.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID 값 대신 User 객체와 직접 관계를 맺음
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private String token;

    public void updateToken(String token){
        this.token = token;
    }

    // 토큰 갱신 로직을 엔티티가 스스로 수행
    public void rotate(String newToken) {
        this.token = newToken;
    }

    // 소유자 일치 여부를 엔티티가 직접 확인
    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
