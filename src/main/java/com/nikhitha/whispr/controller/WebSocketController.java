package com.nikhitha.whispr.controller;

import com.nikhitha.whispr.dto.ChatMessage;
import com.nikhitha.whispr.dto.WebSocketUser;
import com.nikhitha.whispr.service.MessageService;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;


import java.time.LocalDateTime;

@Controller
public class WebSocketController {
     @Autowired
    private SimpMessagingTemplate messagingTemplate;
    

    @Autowired
    private MessageService messageService;

    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        if (chatMessage.getType() == ChatMessage.MessageType.CHAT) {
            messageService.saveMessage(chatMessage);
        }
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, 
                               SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        chatMessage.setTimestamp(LocalDateTime.now());

         messageService.addOnlineUser(chatMessage.getSender());
        
        messagingTemplate.convertAndSend("/topic/online.users", 
            messageService.getOnlineUsers());

        return chatMessage;
    }
    
     @MessageMapping("/chat.leave")
    @SendTo("/topic/public")
    public ChatMessage leaveUser(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        
        messageService.removeOnlineUser(chatMessage.getSender());
        
        messagingTemplate.convertAndSend("/topic/online.users", 
            messageService.getOnlineUsers());
        
        return chatMessage;
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload WebSocketUser user) {
        messagingTemplate.convertAndSend("/topic/typing", user);
    }

    @MessageMapping("/chat.markRead")
    public void markMessagesAsRead(@Payload String roomId, 
                                   SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username != null && roomId != null) {
            messageService.markMessagesAsRead(roomId, username);
        }
    }

    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSendToUser(
            chatMessage.getRoomId(),
            "/queue/messages", 
            chatMessage
        );
    }
}
