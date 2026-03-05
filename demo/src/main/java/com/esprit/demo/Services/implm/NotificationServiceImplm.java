package com.esprit.demo.Services.implm;

import com.esprit.demo.Dto.NotificationResponse;
import com.esprit.demo.Models.Notification;
import com.esprit.demo.Models.User;
import com.esprit.demo.Repositories.NotificationRepository;
import com.esprit.demo.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImplm {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Creates a notification for the recipient. Silently ignores failures
     * so the calling operation is never blocked by notification errors.
     */
    public void create(Long recipientId, String type, String message, Long threadId) {
        if (recipientId == null) return;
        try {
            User recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null) return;
            Notification n = Notification.builder()
                    .recipient(recipient)
                    .type(type)
                    .message(message)
                    .threadId(threadId)
                    .build();
            notificationRepository.save(n);
        } catch (Exception ignored) {}
    }

    public List<NotificationResponse> getByUser(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getType());
        r.setMessage(n.getMessage());
        r.setThreadId(n.getThreadId());
        r.setRead(n.isRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}
