package com.tennis.jeton;

import com.tennis.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/jetons")
@RequiredArgsConstructor
public class JetonController {

    private final JetonRepository jetonRepository;

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> getBalance(@AuthenticationPrincipal User user) {
        int balance = jetonRepository.findByUser(user)
                .map(JetonAccount::getBalance)
                .orElse(0);
        return ResponseEntity.ok(Map.of("balance", balance));
    }

    @PostMapping("/topup")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> topup(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        int amount = Integer.parseInt(body.get("amount").toString());

        jetonRepository.findAll().stream()
                .filter(a -> a.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(a -> {
                    a.setBalance(a.getBalance() + amount);
                    jetonRepository.save(a);
                });

        return ResponseEntity.ok().build();
    }
}
