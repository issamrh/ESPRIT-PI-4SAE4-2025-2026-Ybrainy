package com.esprit.demo.Controllers;

import com.esprit.demo.Dto.UserProfileResponse;
import com.esprit.demo.Dto.XpAwardResult;
import com.esprit.demo.Dto.XpEventResponse;
import com.esprit.demo.Services.implm.XpServiceImplm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final XpServiceImplm xpService;

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable("id") Long id) {
        return ResponseEntity.ok(xpService.getProfile(id));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserProfileResponse>> getLeaderboard() {
        return ResponseEntity.ok(xpService.getLeaderboard());
    }

    @GetMapping("/{id}/xp-history")
    public ResponseEntity<List<XpEventResponse>> getXpHistory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(xpService.getXpHistory(id));
    }

    @GetMapping("/{id}/xp-recent")
    public ResponseEntity<List<XpEventResponse>> getRecentXp(@PathVariable("id") Long id) {
        return ResponseEntity.ok(xpService.getRecentXp(id));
    }

    @PostMapping("/{userId}/best-answer/{postId}")
    public ResponseEntity<XpAwardResult> markBestAnswer(
            @PathVariable("userId") Long userId,
            @PathVariable("postId") Long postId) {
        return ResponseEntity.ok(xpService.awardBestAnswer(userId, postId));
    }

    @PostMapping("/{userId}/upvote/{postId}")
    public ResponseEntity<XpAwardResult> awardUpvote(
            @PathVariable("userId") Long userId,
            @PathVariable("postId") Long postId) {
        return ResponseEntity.ok(xpService.awardUpvoteReceived(userId, postId));
    }
}
