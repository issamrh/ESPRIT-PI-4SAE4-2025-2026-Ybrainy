package com.esprit.demo.Services;

import com.esprit.demo.Dto.ThreadInteractionStatus;
import com.esprit.demo.Dto.ThreadResponse;
import com.esprit.demo.Models.ReactionType;
import com.esprit.demo.Models.VoteType;

import java.util.List;

public interface ThreadInteractionService {

    ThreadInteractionStatus vote(Long threadId, Long userId, VoteType voteType);

    ThreadInteractionStatus react(Long threadId, Long userId, ReactionType reactionType);

    ThreadInteractionStatus toggleWishlist(Long threadId, Long userId);

    ThreadInteractionStatus getStatus(Long threadId, Long userId);

    List<ThreadResponse> getWishlist(Long userId);
}
