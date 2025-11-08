package com.nikhitha.whispr.dto;

import com.nikhitha.whispr.entity.ChatRoom;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatRoomDTO {
    private String name;
    private String description;
    private ChatRoom.RoomType type;
    private Boolean isPrivate = false;
    private Integer maxMembers = 50;
    private LocalDateTime createdAt;
    private String createdBy;
    private Long memberCount;
}
