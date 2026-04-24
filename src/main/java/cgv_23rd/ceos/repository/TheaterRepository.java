package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.enums.Region;
import cgv_23rd.ceos.entity.theater.Theater;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TheaterRepository extends JpaRepository<Theater,Long> {
    List<Theater> findAllByRegion(Region region);
}
