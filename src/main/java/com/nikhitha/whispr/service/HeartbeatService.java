package com.nikhitha.whispr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Manages heartbeat and session activity to prevent stale presence entries.
 * Clients can send periodic heartbeats to refresh their session TTL.
 */
@Service
public class HeartbeatService {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final String USER_ACTIVITY_KEY = "user_activity:";
    private static final String ROOM_ACTIVITY_KEY = "room_activity:";
    private static final long ACTIVITY_TTL_HOURS = 24;

    /**
     * Record user activity (heartbeat).
     * Updates the last activity timestamp and extends TTL for the user session.
     */
    public void recordUserActivity(String username) {
        try {
            String key = USER_ACTIVITY_KEY + username;
            redisTemplate.opsForValue().set(
                    key,
                    LocalDateTime.now().toString(),
                    ACTIVITY_TTL_HOURS,
                    TimeUnit.HOURS
            );
            logger.debug("User activity recorded: {}", username);
        } catch (Exception e) {
            logger.warn("Failed to record user activity: {}", e.getMessage());
        }
    }

    /**
     * Record room-specific user activity.
     * Useful for tracking per-room last seen timestamps.
     */
    public void recordRoomActivity(String roomId, String username) {
        try {
            String key = ROOM_ACTIVITY_KEY + roomId + ":" + username;
            redisTemplate.opsForValue().set(
                    key,
                    LocalDateTime.now().toString(),
                    ACTIVITY_TTL_HOURS,
                    TimeUnit.HOURS
            );
            logger.debug("Room activity recorded: {}@{}", username, roomId);
        } catch (Exception e) {
            logger.warn("Failed to record room activity: {}", e.getMessage());
        }
    }

    /**
     * Get last activity timestamp for a user.
     */
    public LocalDateTime getLastActivity(String username) {
        try {
            String key = USER_ACTIVITY_KEY + username;
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return LocalDateTime.parse(value.toString());
            }
        } catch (Exception e) {
            logger.debug("Failed to get last activity: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get last activity timestamp for a user in a specific room.
     */
    public LocalDateTime getLastRoomActivity(String roomId, String username) {
        try {
            String key = ROOM_ACTIVITY_KEY + roomId + ":" + username;
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return LocalDateTime.parse(value.toString());
            }
        } catch (Exception e) {
            logger.debug("Failed to get room activity: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Check if a user is still active (has had activity in the last N seconds).
     * Defaults to checking within the last 5 minutes.
     */
    public boolean isUserStillActive(String username, long seconds) {
        LocalDateTime lastActivity = getLastActivity(username);
        if (lastActivity == null) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(seconds);
        return lastActivity.isAfter(threshold);
    }

    /**
     * Extend TTL for a user session (called on heartbeat).
     */
    public void extendSessionTTL(String username) {
        recordUserActivity(username);
    }

    /**
     * Extend TTL for a room session.
     */
    public void extendRoomSessionTTL(String roomId, String username) {
        recordRoomActivity(roomId, username);
    }
}
