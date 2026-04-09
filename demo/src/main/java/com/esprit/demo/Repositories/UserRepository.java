package com.esprit.demo.Repositories;

import com.esprit.demo.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
     Optional<User> findByEmail(String email);
     Optional<User> findByUsername(String username);
     boolean existsByUsername(String username);
     boolean existsByEmail(String email);

     /** Top 10 utilisateurs par XP — classement global */
     List<User> findTop10ByOrderByXpDesc();

     /** All users ordered by XP descending (for rank computation) */
     List<User> findAllByOrderByXpDesc();
}
