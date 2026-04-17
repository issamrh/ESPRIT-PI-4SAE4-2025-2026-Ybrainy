package tn.esprit.userservice.service;

import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;

import java.util.List;
import java.util.Optional;

public interface IUserServices {
    User addUser(User user);
    User updateUser(User user);
    Optional<User> getUserById(long idUser);
    List<User> getAllUsers();
    void deleteUser(long idUser);
    Optional<User> findFirstByRole(Role role);
    List<Long> findIdsByRole(Role role);
    List<Long> findAllIds();
}
