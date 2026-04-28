package tn.esprit.userservice.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserServicesImp implements IUserServices {

    private final UserRepository userRepository;

    @Override
    public User addUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> getUserById(long idUser) {
        return userRepository.findById(idUser);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(long idUser) {
        userRepository.deleteById(idUser);
    }

    @Override
    public Optional<User> findFirstByRole(Role role) {
        return userRepository.findFirstByRole(role);
    }

    @Override
    public List<Long> findIdsByRole(Role role) {
        return userRepository.findIdsByRole(role);
    }

    @Override
    public List<Long> findAllIds() {
        return userRepository.findAllIds();
    }
}
