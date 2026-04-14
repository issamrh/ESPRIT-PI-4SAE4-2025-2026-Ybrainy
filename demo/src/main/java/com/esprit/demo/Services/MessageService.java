package com.esprit.demo.Services;

import com.esprit.demo.Dto.ConversationPreview;
import com.esprit.demo.Dto.MessageRequest;
import com.esprit.demo.Dto.MessageResponse;

import java.util.List;

public interface MessageService {
    MessageResponse sendMessage(MessageRequest request);
    List<MessageResponse> getConversation(Long userId1, Long userId2);
    List<ConversationPreview> getInbox(Long userId);
    long countUnread(Long userId);
    MessageResponse markAsRead(Long messageId);
    void markConversationAsRead(Long senderId, Long receiverId);
}
