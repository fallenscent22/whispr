package com.nikhitha.whispr.controller;


import com.nikhitha.whispr.entity.Message;
import com.nikhitha.whispr.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
     @Autowired
    private MessageService messageService;

    @GetMapping("/recent/{roomId}")
    public ResponseEntity<List<Message>> getRecentMessages(@PathVariable String roomId) {
        List<Message> messages = messageService.getRecentMessages(roomId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/history/{roomId}")
    public ResponseEntity<Page<Message>> getMessageHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageService.getMessageHistory(roomId, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/online-users")
    public ResponseEntity<List<String>> getOnlineUsers() {
        List<String> onlineUsers = messageService.getOnlineUsers();
        return ResponseEntity.ok(onlineUsers);
    }

    @PostMapping("/mark-read/{roomId}")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable String roomId, 
                                                   @RequestParam String username) {
        messageService.markMessagesAsRead(roomId, username);
        return ResponseEntity.ok().build();
    }
}
