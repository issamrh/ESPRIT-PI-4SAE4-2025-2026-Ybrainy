package tn.esprit.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findFirstByRole(Role role);

    @Query("select u.idUser from User u where u.role = :role order by u.idUser asc")
    List<Long> findIdsByRole(@Param("role") Role role);

    @Query("select u.idUser from User u order by u.idUser asc")
    List<Long> findAllIds();
}
