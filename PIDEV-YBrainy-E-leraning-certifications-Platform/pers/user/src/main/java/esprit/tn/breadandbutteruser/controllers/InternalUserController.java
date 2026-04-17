package esprit.tn.breadandbutteruser.controllers;

import esprit.tn.breadandbutteruser.dto.InternalUserResponse;
import esprit.tn.breadandbutteruser.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public InternalUserResponse getUserById(@PathVariable Long id) {
        return userService.getInternalUser(id);
    }
}
