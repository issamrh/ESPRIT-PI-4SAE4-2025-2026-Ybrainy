package com.esprit.demo.Services.implm;

import com.esprit.demo.Dto.ConversationPreview;
import com.esprit.demo.Dto.MessageRequest;
import com.esprit.demo.Dto.MessageResponse;
import com.esprit.demo.Models.PrivateMessage;
import com.esprit.demo.Models.User;
import com.esprit.demo.Repositories.PrivateMessageRepository;
import com.esprit.demo.Repositories.UserRepository;
import com.esprit.demo.Services.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageServiceImplm implements MessageService {

    private final PrivateMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Send ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse sendMessage(MessageRequest request) {
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found: " + request.getSenderId()));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + request.getReceiverId()));

        PrivateMessage msg = PrivateMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl())
                .mediaType(request.getMediaType())
                .build();

        msg = messageRepository.save(msg);
        MessageResponse response = toResponse(msg);

        // Push to receiver's WebSocket channel in real-time
        messagingTemplate.convertAndSend(
                "/topic/messages/user-" + receiver.getId(),
                response
        );

        return response;
    }

    // ── Conversation ─────────────────────────────────────────────────────────

    @Override
    public List<MessageResponse> getConversation(Long userId1, Long userId2) {
        return messageRepository.findConversation(userId1, userId2)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Inbox ─────────────────────────────────────────────────────────────────

    @Override
    public List<ConversationPreview> getInbox(Long userId) {
        List<PrivateMessage> all = messageRepository.findAllForUser(userId);

        // Group by conversation partner — first occurrence per partner is the latest message
        Map<Long, ConversationPreview> convMap = new LinkedHashMap<>();

        for (PrivateMessage msg : all) {
            boolean isSender = msg.getSender().getId().equals(userId);
            Long partnerId = isSender ? msg.getReceiver().getId() : msg.getSender().getId();

            if (convMap.containsKey(partnerId)) continue;

            User partner = isSender ? msg.getReceiver() : msg.getSender();
            long unread = messageRepository.countUnreadFromSender(partnerId, userId);

            String preview;
            if (msg.getContent() != null && !msg.getContent().isBlank()) {
                preview = msg.getContent().length() > 60
                        ? msg.getContent().substring(0, 60) + "…"
                        : msg.getContent();
            } else if ("image".equals(msg.getMediaType())) {
                preview = "📷 Image";
            } else if (msg.getMediaType() != null) {
                preview = "📎 File";
            } else {
                preview = "";
            }

            convMap.put(partnerId, ConversationPreview.builder()
                    .userId(partnerId)
                    .username(partner.getUsername())
                    .role(partner.getRole().name())
                    .level(partner.getLevel())
                    .levelTitle(levelTitle(partner.getLevel()))
                    .lastMessage(preview)
                    .lastMessageAt(msg.getCreatedAt())
                    .unreadCount(unread)
                    .sentByMe(isSender)
                    .build());
        }

        return new ArrayList<>(convMap.values());
    }

    // ── Unread count ──────────────────────────────────────────────────────────

    @Override
    public long countUnread(Long userId) {
        return messageRepository.countTotalUnread(userId);
    }

    // ── Mark read ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse markAsRead(Long messageId) {
        PrivateMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));
        msg.setRead(true);
        return toResponse(messageRepository.save(msg));
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long senderId, Long receiverId) {
        messageRepository.markConversationRead(senderId, receiverId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MessageResponse toResponse(PrivateMessage m) {
        return MessageResponse.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .senderUsername(m.getSender().getUsername())
                .senderRole(m.getSender().getRole().name())
                .senderLevel(m.getSender().getLevel())
                .senderLevelTitle(levelTitle(m.getSender().getLevel()))
                .receiverId(m.getReceiver().getId())
                .receiverUsername(m.getReceiver().getUsername())
                .content(m.getContent())
                .mediaUrl(m.getMediaUrl())
                .mediaType(m.getMediaType())
                .read(m.isRead())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private String levelTitle(int level) {
        return switch (level) {
            case 1 -> "Newcomer";
            case 2 -> "Apprentice";
            case 3 -> "Explorer";
            case 4 -> "Contributor";
            case 5 -> "Expert";
            case 6 -> "Senior Expert";
            case 7 -> "Master";
            case 8 -> "Legend";
            default -> "Level " + level;
        };
    }
}
