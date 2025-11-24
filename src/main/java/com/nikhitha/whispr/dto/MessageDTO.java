package com.nikhitha.whispr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Consistent DTO for message responses across REST and WebSocket endpoints.
 * Prevents mixing entity serialization with DTO shapes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    
    @JsonProperty("messageId")
    private Long messageId;
    
    @JsonProperty("type")
    private String type; // "CHAT", "JOIN", "LEAVE", "TYPING", etc.
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("sender")
    private String senderUsername;
    
    @JsonProperty("senderUserId")
    private Long senderUserId;
    
    @JsonProperty("roomId")
    private String roomId;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("isDelivered")
    private Boolean isDelivered;
    
    @JsonProperty("isRead")
    private Boolean isRead;
    
    /**
     * Convert from ChatMessage DTO (used by WebSocket input)
     */
    public static MessageDTO fromChatMessage(ChatMessage chatMessage, Long senderUserId) {
        MessageDTO dto = new MessageDTO();
        dto.setType(chatMessage.getType() != null ? chatMessage.getType().name() : "CHAT");
        dto.setContent(chatMessage.getContent());
        dto.setSenderUsername(chatMessage.getSender());
        dto.setSenderUserId(senderUserId);
        dto.setRoomId(chatMessage.getRoomId() != null ? chatMessage.getRoomId() : "global");
        dto.setTimestamp(chatMessage.getTimestamp() != null ? chatMessage.getTimestamp() : LocalDateTime.now());
        dto.setMessageId(chatMessage.getMessageId());
        dto.setIsDelivered(false);
        dto.setIsRead(false);
        return dto;
    }
    
    /**
     * Convert from Message entity (database model)
     */
    public static MessageDTO fromEntity(com.nikhitha.whispr.entity.Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getId());
        dto.setType(message.getType() != null ? message.getType().name() : "CHAT");
        dto.setContent(message.getContent());
        dto.setSenderUsername(message.getSender() != null ? message.getSender().getUsername() : "unknown");
        dto.setSenderUserId(message.getSender() != null ? message.getSender().getId() : null);
        dto.setRoomId(message.getRoomId() != null ? message.getRoomId() : "global");
        dto.setTimestamp(message.getCreatedAt());
        dto.setIsDelivered(message.getIsDelivered());
        dto.setIsRead(message.getIsRead());
        return dto;
    }
    
    /**
     * Convert to ChatMessage for WebSocket broadcast
     */
    public ChatMessage toChatMessage() {
        ChatMessage msg = new ChatMessage();
        msg.setType(ChatMessage.MessageType.valueOf(this.type));
        msg.setContent(this.content);
        msg.setSender(this.senderUsername);
        msg.setRoomId(this.roomId);
        msg.setTimestamp(this.timestamp);
        msg.setMessageId(this.messageId);
        return msg;
    }
}
