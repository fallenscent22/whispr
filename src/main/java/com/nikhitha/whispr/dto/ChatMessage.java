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

    public enum MessageType {
        CHAT, JOIN, LEAVE, TYPING, STOP_TYPING
    }
}
