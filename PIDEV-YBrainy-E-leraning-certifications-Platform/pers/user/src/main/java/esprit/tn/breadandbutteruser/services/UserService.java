package esprit.tn.breadandbutteruser.services;

import esprit.tn.breadandbutteruser.controllers.XpController;
import esprit.tn.breadandbutteruser.dto.ForumProfileResponse;
import esprit.tn.breadandbutteruser.dto.InternalUserResponse;
import esprit.tn.breadandbutteruser.entities.User;
import esprit.tn.breadandbutteruser.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ForumProfileResponse getForumProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return ForumProfileResponse.fromUser(user);
    }

    @Transactional(readOnly = true)
    public ForumProfileResponse getForumProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return ForumProfileResponse.fromUser(user);
    }

    @Transactional(readOnly = true)
    public List<ForumProfileResponse> getLeaderboard() {
        return userRepository.findTop10ByOrderByXpDesc()
                .stream()
                .map(ForumProfileResponse::fromUser)
                .toList();
    }

    public ForumProfileResponse updateForumProfile(Long userId, XpController.UpdateForumProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (StringUtils.hasText(request.getUsername())) {
            user.setUsername(request.getUsername().trim());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        return ForumProfileResponse.fromUser(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public InternalUserResponse getInternalUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return InternalUserResponse.fromUser(user);
    }
}
