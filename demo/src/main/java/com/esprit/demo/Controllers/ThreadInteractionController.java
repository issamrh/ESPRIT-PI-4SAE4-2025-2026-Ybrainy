package com.esprit.demo.Controllers;

import com.esprit.demo.Dto.ThreadInteractionStatus;
import com.esprit.demo.Dto.ThreadResponse;
import com.esprit.demo.Models.ReactionType;
import com.esprit.demo.Models.VoteType;
import com.esprit.demo.Services.implm.ThreadInteractionServiceImplm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ThreadInteractionController {

    private final ThreadInteractionServiceImplm interactionService;

    /** POST /api/threads/{threadId}/interactions/vote?userId=X
     *  Body: { "voteType": "UPVOTE" | "DOWNVOTE" }
     *  Toggle: same type removes the vote, opposite type switches it.
     */
    @PostMapping("/api/threads/{threadId}/interactions/vote")
    public ResponseEntity<ThreadInteractionStatus> vote(
            @PathVariable("threadId") Long threadId,
            @RequestParam("userId") Long userId,
            @RequestBody Map<String, String> body) {
        VoteType voteType = VoteType.valueOf(body.get("voteType").toUpperCase());
        return ResponseEntity.ok(interactionService.vote(threadId, userId, voteType));
    }

    /** POST /api/threads/{threadId}/interactions/react?userId=X
     *  Body: { "reactionType": "LIKE" | "DISLIKE" }
     *  Toggle: same type removes the reaction, opposite type switches it.
     */
    @PostMapping("/api/threads/{threadId}/interactions/react")
    public ResponseEntity<ThreadInteractionStatus> react(
            @PathVariable("threadId") Long threadId,
            @RequestParam("userId") Long userId,
            @RequestBody Map<String, String> body) {
        ReactionType reactionType = ReactionType.valueOf(body.get("reactionType").toUpperCase());
        return ResponseEntity.ok(interactionService.react(threadId, userId, reactionType));
    }

    /** POST /api/threads/{threadId}/interactions/wishlist?userId=X
     *  Toggles wishlist: adds if not present, removes if already saved.
     */
    @PostMapping("/api/threads/{threadId}/interactions/wishlist")
    public ResponseEntity<ThreadInteractionStatus> toggleWishlist(
            @PathVariable("threadId") Long threadId,
            @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(interactionService.toggleWishlist(threadId, userId));
    }

    /** GET /api/threads/{threadId}/interactions/status?userId=X
     *  Returns the current user's vote, reaction, wishlist status + counts.
     */
    @GetMapping("/api/threads/{threadId}/interactions/status")
    public ResponseEntity<ThreadInteractionStatus> getStatus(
            @PathVariable("threadId") Long threadId,
            @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(interactionService.getStatus(threadId, userId));
    }

    /** GET /api/wishlist?userId=X
     *  Returns the list of threads saved by the user.
     */
    @GetMapping("/api/wishlist")
    public ResponseEntity<List<ThreadResponse>> getWishlist(
            @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(interactionService.getWishlist(userId));
    }
}
