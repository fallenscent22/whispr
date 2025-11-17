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
    private KafkaProducerService kafkaProducerService;

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
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        logger.debug("User joining: {}", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        chatMessage.setTimestamp(LocalDateTime.now());

        // Add to online users
        messageService.addOnlineUser(chatMessage.getSender());
        if (presenceService != null) {
            presenceService.userConnected(chatMessage.getSender(), headerAccessor.getSessionId());
        }

        // Broadcast updated online users
        messagingTemplate.convertAndSend("/topic/online.users", messageService.getOnlineUsers());

        return chatMessage;
    }

    @MessageMapping("/chat.leave")
    @SendTo("/topic/public")
    public ChatMessage leaveUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        logger.debug("User leaving: {}", chatMessage.getSender());
        chatMessage.setTimestamp(LocalDateTime.now());

        // Remove from online users
        messageService.removeOnlineUser(chatMessage.getSender());
        if (presenceService != null) {
            presenceService.userDisconnected(chatMessage.getSender(), headerAccessor.getSessionId());
        }

        // Broadcast updated online users
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

    // Inner classes for WebSocket messages
    @Data
    public static class TypingRequest {
        private String roomId;
    }

    @Data
    public static class ReadReceiptRequest {
        private Long messageId;
        private String roomId;
    }

    @Data
    public static class DeliveryReceiptRequest {
        private Long messageId;
        private String roomId;
    }
}