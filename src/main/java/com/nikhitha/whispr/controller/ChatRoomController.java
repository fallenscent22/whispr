package com.nikhitha.whispr.controller;

import com.nikhitha.whispr.dto.ChatRoomDTO;
import com.nikhitha.whispr.entity.ChatRoom;
import com.nikhitha.whispr.entity.RoomMember;
import com.nikhitha.whispr.entity.User;
import com.nikhitha.whispr.service.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatrooms")
public class ChatRoomController {
     @Autowired
    private ChatRoomService chatRoomService;

    @PostMapping
    public ResponseEntity<?> createChatRoom(@RequestBody ChatRoomDTO chatRoomDTO, Authentication authentication) {
        try {
            String username = authentication.getName();
            ChatRoom chatRoom = chatRoomService.createChatRoom(chatRoomDTO, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Chat room created successfully");
            response.put("roomId", chatRoom.getRoomId());
            response.put("room", chatRoom);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-rooms")
    public ResponseEntity<Page<ChatRoom>> getUserChatRooms(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        String username = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> chatRooms = chatRoomService.getUserChatRooms(username, pageable);
        return ResponseEntity.ok(chatRooms);
    }

    @GetMapping("/discover")
    public ResponseEntity<Page<ChatRoom>> discoverPublicRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> chatRooms = chatRoomService.discoverPublicRooms(pageable);
        return ResponseEntity.ok(chatRooms);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ChatRoom>> searchRooms(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> chatRooms = chatRoomService.searchRooms(q, pageable);
        return ResponseEntity.ok(chatRooms);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getChatRoom(@PathVariable String roomId) {
        try {
            ChatRoom chatRoom = chatRoomService.getChatRoomByRoomId(roomId);
            return ResponseEntity.ok(chatRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<List<User>> getRoomMembers(@PathVariable String roomId) {
        List<User> members = chatRoomService.getRoomMembers(roomId);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinChatRoom(@PathVariable String roomId, Authentication authentication) {
        try {
            String username = authentication.getName();
            ChatRoom chatRoom = chatRoomService.getChatRoomByRoomId(roomId);
            
            // Check if room is private
            if (chatRoom.getIsPrivate()) {
                return ResponseEntity.badRequest().body(Map.of("error", "This is a private room"));
            }
            
            User user = chatRoom.getMembers().stream()
                    .filter(member -> member.getUser().getUsername().equals(username))
                    .findFirst()
                    .map(member -> member.getUser())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            chatRoomService.addMemberToRoom(chatRoom, user, RoomMember.MemberRole.MEMBER);
            
            return ResponseEntity.ok(Map.of("message", "Joined chat room successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveChatRoom(@PathVariable String roomId, Authentication authentication) {
        try {
            String username = authentication.getName();
            chatRoomService.removeMemberFromRoom(roomId, username, username);
            return ResponseEntity.ok(Map.of("message", "Left chat room successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<?> updateChatRoom(@PathVariable String roomId, 
                                           @RequestBody ChatRoomDTO chatRoomDTO,
                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            ChatRoom updatedRoom = chatRoomService.updateChatRoom(roomId, chatRoomDTO, username);
            return ResponseEntity.ok(updatedRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
