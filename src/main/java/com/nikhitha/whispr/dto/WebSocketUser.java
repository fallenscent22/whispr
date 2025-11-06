package com.nikhitha.whispr.dto;

import lombok.Data;

@Data
public class WebSocketUser {
    private String username;
    private String sessionId;
    private boolean typing;
}
