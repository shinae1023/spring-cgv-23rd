package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}
