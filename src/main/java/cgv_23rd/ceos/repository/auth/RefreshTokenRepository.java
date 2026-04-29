package cgv_23rd.ceos.repository.auth;

import cgv_23rd.ceos.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String refreshToken);

    Optional<RefreshToken> findByUser_Id(Long userId);

}
