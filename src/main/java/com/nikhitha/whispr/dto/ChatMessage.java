package com.nikhitha.whispr.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private MessageType type;
    private String content;
    private String sender;
    private String roomId;
    private LocalDateTime timestamp;
    private Long messageId; // Add this field

    public enum MessageType {
        CHAT, JOIN, LEAVE, TYPING, STOP_TYPING
    }

    // Add getter and setter for messageId if not using Lombok
    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }
}