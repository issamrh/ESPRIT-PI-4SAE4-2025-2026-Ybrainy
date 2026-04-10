package com.esprit.demo.Dto;

import com.esprit.demo.Models.ReactionType;
import com.esprit.demo.Models.VoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadInteractionStatus {

    private VoteType userVote;        // null if no vote
    private ReactionType userReaction; // null if no reaction
    private boolean wishlisted;

    private long upvoteCount;
    private long downvoteCount;
    private long voteScore;

    private long likeCount;
    private long dislikeCount;
}
