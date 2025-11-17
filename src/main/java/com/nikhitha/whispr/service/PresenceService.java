package com.nikhitha.whispr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class PresenceService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final String ONLINE_USERS_KEY = "online_users";
    private static final String USER_SESSIONS_KEY = "user_sessions:";
    private static final String LAST_SEEN_KEY = "last_seen:";

    public void userConnected(String username, String sessionId) {
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, username);

        String userSessionsKey = USER_SESSIONS_KEY + username;
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, 1, TimeUnit.DAYS);

        updateLastSeen(username);

        broadcastPresenceUpdate(username, true);
    }

    public void userDisconnected(String username, String sessionId) {
        String userSessionsKey = USER_SESSIONS_KEY + username;
        redisTemplate.opsForSet().remove(userSessionsKey, sessionId);

        Long remainingSessions = redisTemplate.opsForSet().size(userSessionsKey);
        if (remainingSessions == null || remainingSessions == 0) {
            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, username);
            updateLastSeen(username);
            broadcastPresenceUpdate(username, false);
        }
    }

    public boolean isUserOnline(String username) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, username));
    }

    public Set<String> getOnlineUsers() {
        Set<Object> onlineUsers = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        return onlineUsers != null
                ? onlineUsers.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet())
                : new HashSet<>();
    }

    public LocalDateTime getLastSeen(String username) {
        Object lastSeen = redisTemplate.opsForValue().get(LAST_SEEN_KEY + username);
        if (lastSeen != null) {
            return LocalDateTime.parse(lastSeen.toString());
        }
        return null;
    }

    private void updateLastSeen(String username) {
        redisTemplate.opsForValue().set(
                LAST_SEEN_KEY + username,
                LocalDateTime.now().toString(),
                30, TimeUnit.DAYS);
    }

    private void broadcastPresenceUpdate(String username, boolean isOnline) {
        PresenceUpdate presenceUpdate = new PresenceUpdate(username, isOnline,
                isOnline ? null : getLastSeen(username));

        messagingTemplate.convertAndSend("/topic/presence", presenceUpdate);
    }

    public static class PresenceUpdate {
        private String username;
        private boolean online;
        private LocalDateTime lastSeen;
        private LocalDateTime timestamp;

        public PresenceUpdate(String username, boolean online, LocalDateTime lastSeen) {
            this.username = username;
            this.online = online;
            this.lastSeen = lastSeen;
            this.timestamp = LocalDateTime.now();
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public boolean isOnline() { return online; }
        public void setOnline(boolean online) { this.online = online; }
        public LocalDateTime getLastSeen() { return lastSeen; }
        public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}