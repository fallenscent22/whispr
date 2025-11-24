package com.nikhitha.whispr.controller;

import com.nikhitha.whispr.service.MessageService;
import com.nikhitha.whispr.service.PresenceService;
import com.nikhitha.whispr.service.RoomPresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Debug endpoints for monitoring application health, persistence, and message/Kafka status.
 * Only for development/testing; consider securing in production.
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private RoomPresenceService roomPresenceService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Check persistence: return count of messages and sample of recent ones.
     */
    @GetMapping("/messages/count")
    public ResponseEntity<Map<String, Object>> getMessageCount() {
        try {
            // Get global room recent messages
            List<com.nikhitha.whispr.entity.Message> messages = messageService.getRecentMessages("global");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("recentMessagesCount", messages.size());
            response.put("latestMessage", messages.isEmpty() ? null : messages.get(messages.size() - 1));
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Check Redis/online users health.
     */
    @GetMapping("/presence/status")
    public ResponseEntity<Map<String, Object>> getPresenceStatus() {
        try {
            List<String> onlineUsers = messageService.getOnlineUsers();
            Set<String> presenceUsers = presenceService.getOnlineUsers();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("onlineUsersCount", onlineUsers.size());
            response.put("onlineUsers", onlineUsers);
            response.put("presenceUsersCount", presenceUsers.size());
            response.put("presenceUsers", presenceUsers);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Check Kafka connectivity by attempting to send a test message.
     */
    @GetMapping("/kafka/health")
    public ResponseEntity<Map<String, Object>> getKafkaHealth() {
        Map<String, Object> response = new HashMap<>();
        
        if (kafkaTemplate == null) {
            response.put("status", "disabled");
            response.put("message", "Kafka template not available");
            return ResponseEntity.ok(response);
        }

        try {
            // Try to send a test message
            String testMsg = "{\"test\": true, \"timestamp\": \"" + java.time.LocalDateTime.now() + "\"}";
            kafkaTemplate.send("whispr-messages", "test-key", testMsg);
            
            response.put("status", "ok");
            response.put("message", "Kafka is reachable");
            response.put("testMessageSent", true);
            response.put("topic", "whispr-messages");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to connect to Kafka: " + e.getMessage());
            response.put("testMessageSent", false);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Check Redis connectivity.
     */
    @GetMapping("/redis/health")
    public ResponseEntity<Map<String, Object>> getRedisHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            response.put("status", "ok");
            response.put("message", "Redis is reachable");
            response.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to connect to Redis: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get aggregate application health.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getAppHealth() {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> components = new HashMap<>();
        
        // Check Redis
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            components.put("redis", "ok");
        } catch (Exception e) {
            components.put("redis", "error: " + e.getMessage());
        }

        // Check Kafka
        if (kafkaTemplate == null) {
            components.put("kafka", "disabled");
        } else {
            try {
                kafkaTemplate.send("whispr-messages", "health-check", "{}");
                components.put("kafka", "ok");
            } catch (Exception e) {
                components.put("kafka", "error: " + e.getMessage());
            }
        }

        // Summary
        boolean allOk = components.values().stream().allMatch(v -> v.equals("ok") || v.equals("disabled"));
        response.put("status", allOk ? "ok" : "degraded");
        response.put("components", components);
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}
