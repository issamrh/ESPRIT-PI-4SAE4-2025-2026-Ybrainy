package tn.esprit.userservice.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.service.IUserServices;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(
        origins = {"http://localhost:4200", "http://127.0.0.1:4200"},
        allowCredentials = "true"
)
@AllArgsConstructor
@RequestMapping("/User")
public class UserRestControllers {

    private final IUserServices userServices;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @PostMapping("/add")
    public User addUser(@RequestBody User user) {
        return userServices.addUser(user);
    }

    @PutMapping("/update")
    public User updateUser(@RequestBody User user) {
        return userServices.updateUser(user);
    }

    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userServices.getAllUsers();
    }

    @GetMapping("/{id}")
    public Optional<User> getUserById(@PathVariable("id") long id) {
        return userServices.getUserById(id);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteUser(@PathVariable("id") long id) {
        userServices.deleteUser(id);
    }

    // ── Endpoints consumed by other microservices via Feign ──────────────────

    /**
     * Returns the first user found with the given role.
     * Consumed by event-service when resolving a fallback student.
     */
    @GetMapping("/first-by-role")
    public Optional<User> findFirstByRole(@RequestParam("role") String role) {
        try {
            return userServices.findFirstByRole(Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }
    }

    /**
     * Returns all user IDs for a given role.
     * Consumed by inscription-service to populate the student list.
     */
    @GetMapping("/ids-by-role")
    public List<Long> findIdsByRole(@RequestParam("role") String role) {
        try {
            return userServices.findIdsByRole(Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }
    }

    /**
     * Returns all user IDs regardless of role.
     * Consumed by inscription-service as a fallback.
     */
    @GetMapping("/all-ids")
    public List<Long> findAllIds() {
        return userServices.findAllIds();
    }
}
