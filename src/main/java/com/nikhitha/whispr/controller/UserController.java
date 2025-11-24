package com.nikhitha.whispr.controller;

import com.nikhitha.whispr.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<List<String>> searchUsers(@RequestParam("q") String query) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        List<String> usernames = userRepository.findByUsernameContainingIgnoreCase(query)
                .stream()
                .map(u -> u.getUsername())
                .collect(Collectors.toList());

        return ResponseEntity.ok(usernames);
    }
}
