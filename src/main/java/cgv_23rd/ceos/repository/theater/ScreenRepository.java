package cgv_23rd.ceos.repository.theater;

import cgv_23rd.ceos.entity.theater.Screen;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScreenRepository extends JpaRepository<Screen,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Screen s WHERE s.id = :screenId")
    Optional<Screen> findByIdWithLock(@Param("screenId") Long screenId);
}
