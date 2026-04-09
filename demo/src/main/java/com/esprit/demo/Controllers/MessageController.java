package com.esprit.demo.Controllers;

import com.esprit.demo.Dto.ConversationPreview;
import com.esprit.demo.Dto.MessageRequest;
import com.esprit.demo.Dto.MessageResponse;
import com.esprit.demo.Dto.UserSummary;
import com.esprit.demo.Repositories.UserRepository;
import com.esprit.demo.Services.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ── REST endpoints ────────────────────────────────────────────────────────

    @PostMapping
    @ResponseBody
    public ResponseEntity<MessageResponse> send(@RequestBody MessageRequest request) {
        return ResponseEntity.ok(messageService.sendMessage(request));
    }

    @GetMapping("/conversation/{userId1}/{userId2}")
    @ResponseBody
    public ResponseEntity<List<MessageResponse>> getConversation(
            @PathVariable("userId1") Long userId1,
            @PathVariable("userId2") Long userId2) {
        return ResponseEntity.ok(messageService.getConversation(userId1, userId2));
    }

    @GetMapping("/inbox/{userId}")
    @ResponseBody
    public ResponseEntity<List<ConversationPreview>> getInbox(
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(messageService.getInbox(userId));
    }

    @GetMapping("/unread/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> countUnread(
            @PathVariable("userId") Long userId) {
        return ResponseEntity.ok(Map.of("count", messageService.countUnread(userId)));
    }

    @PutMapping("/read/{messageId}")
    @ResponseBody
    public ResponseEntity<MessageResponse> markAsRead(
            @PathVariable("messageId") Long messageId) {
        return ResponseEntity.ok(messageService.markAsRead(messageId));
    }

    @PutMapping("/conversation/read/{senderId}/{receiverId}")
    @ResponseBody
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable("senderId") Long senderId,
            @PathVariable("receiverId") Long receiverId) {
        messageService.markConversationAsRead(senderId, receiverId);
        return ResponseEntity.ok().build();
    }

    /** List all users (for starting new conversations) */
    @GetMapping("/users")
    @ResponseBody
    public ResponseEntity<List<UserSummary>> getAllUsers() {
        List<UserSummary> summaries = userRepository.findAll().stream()
                .map(u -> new UserSummary(
                        u.getId(),
                        u.getUsername(),
                        u.getRole().name(),
                        u.getLevel(),
                        levelTitle(u.getLevel())))
                .toList();
        return ResponseEntity.ok(summaries);
    }

    /** Upload a file attachment for messages */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadMedia(
            @RequestPart("file") MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir, "messages").toAbsolutePath();
        Files.createDirectories(dir);

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + ext;

        Files.write(dir.resolve(filename), file.getBytes());

        String url = "/uploads/messages/" + filename;
        String type = file.getContentType() != null && file.getContentType().startsWith("image/")
                ? "image" : "file";

        return ResponseEntity.ok(Map.of("url", url, "type", type, "name", original));
    }

    // ── WebSocket @MessageMapping ─────────────────────────────────────────────

    /** Clients can also send via STOMP: /app/chat.send */
    @MessageMapping("/chat.send")
    public void handleWebSocket(MessageRequest request) {
        messageService.sendMessage(request);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

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
