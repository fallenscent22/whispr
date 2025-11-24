package com.nikhitha.whispr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages per-room online user presence using Redis.
 * Each room tracks its own set of online members.
 */
@Service
public class RoomPresenceService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final String ROOM_USERS_KEY = "room_users:";
    private static final String ROOM_LAST_SEEN_KEY = "room_last_seen:";

    /**
     * Add user to a specific room's online set.
     */
    public void userJoinedRoom(String roomId, String username) {
        String key = ROOM_USERS_KEY + roomId;
        redisTemplate.opsForSet().add(key, username);
        
        // Broadcast updated room users
        broadcastRoomUsers(roomId);
    }

    /**
     * Remove user from a specific room's online set.
     */
    public void userLeftRoom(String roomId, String username) {
        String key = ROOM_USERS_KEY + roomId;
        redisTemplate.opsForSet().remove(key, username);
        
        // Update last seen in room context
        updateLastSeenInRoom(roomId, username);
        
        // Broadcast updated room users
        broadcastRoomUsers(roomId);
    }

    /**
     * Get all online users in a specific room.
     */
    public Set<String> getRoomOnlineUsers(String roomId) {
        String key = ROOM_USERS_KEY + roomId;
        Set<Object> members = redisTemplate.opsForSet().members(key);
        return members != null
                ? members.stream().map(Object::toString).collect(Collectors.toSet())
                : new HashSet<>();
    }

    /**
     * Check if a user is online in a specific room.
     */
    public boolean isUserOnlineInRoom(String roomId, String username) {
        String key = ROOM_USERS_KEY + roomId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, username));
    }

    /**
     * Get the count of online users in a room.
     */
    public long getRoomUserCount(String roomId) {
        String key = ROOM_USERS_KEY + roomId;
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0;
    }

    /**
     * Clear all users from a room (useful for room cleanup).
     */
    public void clearRoomUsers(String roomId) {
        String key = ROOM_USERS_KEY + roomId;
        redisTemplate.delete(key);
    }

    /**
     * Update last-seen timestamp for a user in a room context.
     */
    private void updateLastSeenInRoom(String roomId, String username) {
        String key = ROOM_LAST_SEEN_KEY + roomId + ":" + username;
        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString());
    }

    /**
     * Get the last-seen timestamp for a user in a room.
     */
    public LocalDateTime getLastSeenInRoom(String roomId, String username) {
        String key = ROOM_LAST_SEEN_KEY + roomId + ":" + username;
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            try {
                return LocalDateTime.parse(value.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Broadcast current room online users to all subscribers of that room.
     */
    private void broadcastRoomUsers(String roomId) {
        Set<String> onlineUsers = getRoomOnlineUsers(roomId);
        RoomUsersUpdate update = new RoomUsersUpdate(roomId, onlineUsers, LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/room." + roomId + ".users", update);
    }

    /**
     * DTO for room users updates.
     */
    public static class RoomUsersUpdate {
        private String roomId;
        private Set<String> onlineUsers;
        private LocalDateTime timestamp;

        public RoomUsersUpdate(String roomId, Set<String> onlineUsers, LocalDateTime timestamp) {
            this.roomId = roomId;
            this.onlineUsers = onlineUsers;
            this.timestamp = timestamp;
        }

        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
        public Set<String> getOnlineUsers() { return onlineUsers; }
        public void setOnlineUsers(Set<String> onlineUsers) { this.onlineUsers = onlineUsers; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
