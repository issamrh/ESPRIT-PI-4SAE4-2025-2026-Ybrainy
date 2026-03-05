package esprit.tn.breadandbutteruser.controllers;

import esprit.tn.breadandbutteruser.dto.BanAppealRequestDto;
import esprit.tn.breadandbutteruser.dto.BanAppealResponseDto;
import esprit.tn.breadandbutteruser.services.AuthorizationHelper;
import esprit.tn.breadandbutteruser.services.BanAppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ban-appeals")
@RequiredArgsConstructor
public class BanAppealController {

    private final BanAppealService banAppealService;
    private final AuthorizationHelper authorizationHelper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BanAppealResponseDto> submit(@Valid @RequestBody BanAppealRequestDto request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return new ResponseEntity<>(
                banAppealService.submit(request, authorizationHelper.requireCurrentUser(jwt)),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isBanAppealOwner(authentication, #id)")
    public ResponseEntity<BanAppealResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(banAppealService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BanAppealResponseDto>> getAll() {
        return ResponseEntity.ok(banAppealService.getAll());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelfUserId(authentication, #userId)")
    public ResponseEntity<List<BanAppealResponseDto>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(banAppealService.getByUserId(userId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BanAppealResponseDto>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(banAppealService.getByStatus(status));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BanAppealResponseDto> approve(@PathVariable Long id,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(banAppealService.approve(id, authorizationHelper.actorName(jwt)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BanAppealResponseDto> reject(@PathVariable Long id,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(banAppealService.reject(id, authorizationHelper.actorName(jwt)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isBanAppealOwner(authentication, #id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        banAppealService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
