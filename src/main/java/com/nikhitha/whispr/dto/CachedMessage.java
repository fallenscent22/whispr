package com.nikhitha.whispr.dto;

import com.nikhitha.whispr.entity.Message;
import com.nikhitha.whispr.entity.User;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CachedMessage {
    private Long id;
    private String content;
    private String type;
    private String sender;
    private String roomId;
    private LocalDateTime createdAt;
    private Boolean isDelivered;
    private Boolean isRead;
    
    public static CachedMessage fromEntity(Message message) {
        return new CachedMessage(
            message.getId(),
            message.getContent(),
            message.getType().name(),
            message.getSender().getUsername(),
            message.getRoomId(),
            message.getCreatedAt(),
            message.getIsDelivered(),
            message.getIsRead()
        );
    }
    
    public Message toEntity(User sender) {
        Message message = new Message();
        message.setId(this.id);
        message.setContent(this.content);
        message.setType(Message.MessageType.valueOf(this.type));
        message.setSender(sender);
        message.setRoomId(this.roomId);
        message.setCreatedAt(this.createdAt);
        message.setIsDelivered(this.isDelivered);
        message.setIsRead(this.isRead);
        return message;
    }
}
