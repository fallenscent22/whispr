package com.nikhitha.whispr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TypingService {
     @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final String TYPING_KEY = "typing:";
    private static final long TYPING_TIMEOUT = 3000; // 3 seconds

    public void startTyping(String roomId, String username) {
        String key = TYPING_KEY + roomId;
        redisTemplate.opsForHash().put(key, username, System.currentTimeMillis());
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
        
        // Broadcast typing event
        messagingTemplate.convertAndSend("/topic/typing." + roomId, 
            new TypingEvent(username, roomId, true));
    }

    public void stopTyping(String roomId, String username) {
        String key = TYPING_KEY + roomId;
        redisTemplate.opsForHash().delete(key, username);
        
        // Broadcast stop typing event
        messagingTemplate.convertAndSend("/topic/typing." + roomId, 
            new TypingEvent(username, roomId, false));
    }

    public void checkAndCleanTypingIndicators() {
        // This could be called periodically to clean up stale typing indicators
        // For now, we'll rely on the frontend to send stopTyping events
    }

    public static class TypingEvent {
        private String username;
        private String roomId;
        private boolean typing;
        private long timestamp;

        public TypingEvent(String username, String roomId, boolean typing) {
            this.username = username;
            this.roomId = roomId;
            this.typing = typing;
            this.timestamp = System.currentTimeMillis();
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
        public boolean isTyping() { return typing; }
        public void setTyping(boolean typing) { this.typing = typing; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
