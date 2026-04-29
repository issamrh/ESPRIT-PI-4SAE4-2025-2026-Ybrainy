package tn.esprit.quizservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes/gaze")
public class GazeController {

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startSession(
            @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "sessionId", UUID.randomUUID().toString(),
                "active", false
        ));
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String sessionId) {
        return ResponseEntity.ok(Map.of(
                "user_id", "",
                "active", false,
                "looking_at_screen", false,
                "current_focus_score", 0.0
        ));
    }

    @PostMapping("/stop/{sessionId}")
    public ResponseEntity<Map<String, Object>> stopSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "active", false,
                "stopped", true
        ));
    }
}
