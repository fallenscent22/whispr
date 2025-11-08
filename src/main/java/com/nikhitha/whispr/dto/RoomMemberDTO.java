package com.nikhitha.whispr.dto;

import com.nikhitha.whispr.entity.RoomMember;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoomMemberDTO {
    private String username;
    private RoomMember.MemberRole role;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
}
