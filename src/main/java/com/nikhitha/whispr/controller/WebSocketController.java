package com.nikhitha.whispr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikhitha.whispr.dto.ChatMessage;
import com.nikhitha.whispr.dto.WebSocketUser;
import com.nikhitha.whispr.service.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class WebSocketController {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    @Autowired
    private TypingService typingService;

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private RoomPresenceService roomPresenceService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private HeartbeatService heartbeatService;

    @Autowired
    private ObjectMapper objectMapper;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        logger.debug("Received chat message: {}", chatMessage);
        try {
            chatMessage.setTimestamp(LocalDateTime.now());
            String username = (String) headerAccessor.getSessionAttributes().get("username");
            
            if (username != null && !username.equals(chatMessage.getSender())) {
                logger.warn("Username mismatch: session={}, message={}", username, chatMessage.getSender());
            }

            // Send to Kafka for processing
            String messageJson = objectMapper.writeValueAsString(chatMessage);
            kafkaProducerService.publishMessageEvent(messageJson);
            
        } catch (Exception e) {
            logger.error("Failed to process chat message", e);
            try {
                if (chatMessage.getType() == ChatMessage.MessageType.CHAT) {
                    messageService.saveMessage(chatMessage);
                }
                messagingTemplate.convertAndSend("/topic/public", chatMessage);
            } catch (Exception ex) {
                logger.error("Fallback also failed", ex);
            }
        }
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        logger.debug("User joining: {}", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        chatMessage.setTimestamp(LocalDateTime.now());

        // Add to global online users
        messageService.addOnlineUser(chatMessage.getSender());
        if (presenceService != null) {
            presenceService.userConnected(chatMessage.getSender(), headerAccessor.getSessionId());
        }

        // If the client provided a roomId, also track user in that room
        String roomId = chatMessage.getRoomId();
        if (roomId == null) {
            roomId = "global";
        }
        if (roomPresenceService != null) {
            roomPresenceService.userJoinedRoom(roomId, chatMessage.getSender());
        }

        // Broadcast updated global online users
        messagingTemplate.convertAndSend("/topic/online.users", messageService.getOnlineUsers());

        // If the client provided a roomId, also send a join event into that room topic
        if (chatMessage.getRoomId() != null && !"global".equals(chatMessage.getRoomId())) {
            messagingTemplate.convertAndSend("/topic/room." + chatMessage.getRoomId(), chatMessage);
        } else {
            // fallback: also send to public topic for compatibility
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        }
    }

    @MessageMapping("/chat.leave")
    @SendTo("/topic/public")
    public ChatMessage leaveUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        logger.debug("User leaving: {}", chatMessage.getSender());
        chatMessage.setTimestamp(LocalDateTime.now());

        // Remove from global online users
        messageService.removeOnlineUser(chatMessage.getSender());
        if (presenceService != null) {
            presenceService.userDisconnected(chatMessage.getSender(), headerAccessor.getSessionId());
        }

        // Remove from room-specific presence
        String roomId = chatMessage.getRoomId();
        if (roomId == null) {
            roomId = "global";
        }
        if (roomPresenceService != null) {
            roomPresenceService.userLeftRoom(roomId, chatMessage.getSender());
        }

        // Broadcast updated global online users
        messagingTemplate.convertAndSend("/topic/online.users", messageService.getOnlineUsers());

        return chatMessage;
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload WebSocketUser user, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username != null) {
            user.setUsername(username);
            messagingTemplate.convertAndSend("/topic/typing", user);
        }
    }

    @MessageMapping("/chat.markRead")
    public void markMessagesAsRead(@Payload String roomId, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username != null && roomId != null) {
            messageService.markMessagesAsRead(roomId, username);
        }
    }

    @MessageMapping("/chat.typing.start")
    public void startTyping(@Payload TypingRequest typingRequest, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username != null) {
            typingService.startTyping(typingRequest.getRoomId(), username);
        }
    }

    @MessageMapping("/chat.typing.stop")
    public void stopTyping(@Payload TypingRequest typingRequest, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username != null) {
            typingService.stopTyping(typingRequest.getRoomId(), username);
        }
    }

    @MessageMapping("/chat.message.read")
    public void markMessageAsRead(@Payload ReadReceiptRequest readReceiptRequest, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username != null) {
            messageService.markMessageAsRead(readReceiptRequest.getMessageId(), username);
        }
    }

    @MessageMapping("/heartbeat")
    public void handleHeartbeat(@Payload HeartbeatRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username != null && heartbeatService != null) {
            heartbeatService.recordUserActivity(username);
            if (request.getRoomId() != null) {
                heartbeatService.recordRoomActivity(request.getRoomId(), username);
            }
            logger.debug("Heartbeat received from {}", username);
        }
    }

    // Inner classes for WebSocket messages
    @Data
    static class TypingRequest {
        private String roomId;
    }

    @Data
    static class ReadReceiptRequest {
        private Long messageId;
        private String roomId;
    }

    @Data
    static class DeliveryReceiptRequest {
        private Long messageId;
        private String roomId;
    }

    @Data
    static class HeartbeatRequest {
        private String roomId;
    }
}
