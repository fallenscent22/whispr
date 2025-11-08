package com.nikhitha.whispr.service;

import com.nikhitha.whispr.dto.ChatMessage;
import com.nikhitha.whispr.entity.Message;
import com.nikhitha.whispr.entity.RoomMember;
import com.nikhitha.whispr.entity.User;
import com.nikhitha.whispr.repository.MessageRepository;
import com.nikhitha.whispr.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.nikhitha.whispr.repository.RoomMemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MessageService {
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String RECENT_MESSAGES_KEY = "recent_messages:";
    private static final String ONLINE_USERS_KEY = "online_users";
    private static final long CACHE_EXPIRY_HOURS = 24;

    @Transactional
    public Message saveMessage(ChatMessage chatMessage) {
        User sender = userRepository.findByUsername(chatMessage.getSender())
                .orElseThrow(() -> new RuntimeException("User not found: " + chatMessage.getSender()));

        Message message = new Message();
        message.setContent(chatMessage.getContent());
        message.setType(Message.MessageType.valueOf(chatMessage.getType().name()));
        message.setSender(sender);
        message.setRoomId(chatMessage.getRoomId() != null ? chatMessage.getRoomId() : "global");
        message.setCreatedAt(chatMessage.getTimestamp());

        Message savedMessage = messageRepository.save(message);

        cacheMessage(savedMessage);

        return savedMessage;
    }

    @Transactional(readOnly = true)
    public List<Message> getRecentMessages(String roomId) {
        String cacheKey = RECENT_MESSAGES_KEY + roomId;
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        List<Message> cachedMessages = null;

        if (cachedData instanceof List<?>) {
            cachedMessages = ((List<?>) cachedData).stream()
                    .filter(obj -> obj instanceof Message)
                    .map(obj -> (Message) obj)
                    .toList();
        }

        if (cachedMessages != null && !cachedMessages.isEmpty()) {
            return cachedMessages;
        }
        List<Message> messages = messageRepository.findTop50ByRoomIdOrderByCreatedAtDesc(roomId);
        redisTemplate.opsForValue().set(cacheKey, messages, CACHE_EXPIRY_HOURS, TimeUnit.HOURS);

        return messages;
    }

    @Transactional(readOnly = true)
    public Page<Message> getMessageHistory(String roomId, Pageable pageable) {
        return messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
    }

    private void cacheMessage(Message message) {
        String cacheKey = RECENT_MESSAGES_KEY + (message.getRoomId() != null ? message.getRoomId() : "global");

        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        List<Message> cachedMessages = null;

        if (cachedData instanceof List<?>) {
            cachedMessages = ((List<?>) cachedData).stream()
                    .filter(obj -> obj instanceof Message)
                    .map(obj -> (Message) obj)
                    .toList();
        }

        if (cachedMessages != null) {
            cachedMessages.add(0, message);
            if (cachedMessages.size() > 50) {
                cachedMessages = cachedMessages.subList(0, 50);
            }
            redisTemplate.opsForValue().set(cacheKey, cachedMessages, CACHE_EXPIRY_HOURS, TimeUnit.HOURS);
        }
    }

    public void addOnlineUser(String username) {
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, username);
        redisTemplate.expire(ONLINE_USERS_KEY, CACHE_EXPIRY_HOURS, TimeUnit.HOURS);
    }

    public void removeOnlineUser(String username) {
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, username);
    }

    public List<String> getOnlineUsers() {
        return redisTemplate.opsForSet().members(ONLINE_USERS_KEY)
                .stream()
                .map(Object::toString)
                .toList();
    }

    public boolean isUserOnline(String username) {
        return redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, username);
    }

    @Transactional
    public void markMessageAsRead(Long messageId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!isUserInRoom(user.getId(), message.getRoomId())) {
            throw new RuntimeException("User is not a member of this room");
        }

        message.markAsReadBy(user.getId());
        messageRepository.save(message);

        messagingTemplate.convertAndSend("/topic/read-receipt." + message.getRoomId(),
                new ReadReceipt(messageId, username, System.currentTimeMillis()));
    }

    @Transactional
    public void markMessagesAsRead(String roomId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Message> unreadMessages = messageRepository.findUnreadMessagesInRoom(roomId, user.getId());

        for (Message message : unreadMessages) {
            message.setIsDelivered(true);
            message.markAsReadBy(user.getId());
        }

        messageRepository.saveAll(unreadMessages);
        updateLastReadTimestamp(roomId, username);
    }

    @Transactional
    public void markMessageAsDelivered(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setIsDelivered(true);
        messageRepository.save(message);
    }

    private boolean isUserInRoom(Long userId, String roomId) {
        //for global room, all users are considered members
        if("global".equals(roomId)) {
            return true;
        }
        
        // for other rooms check membership
        return roomMemberRepository.findByRoomIdAndUserId(roomId, userId).isPresent();
    }

    private void updateLastReadTimestamp(String roomId, String username) {

        // for global room, skip room member check
        if("global".equals(roomId)) {
            return; 
        }
        
        RoomMember roomMember = roomMemberRepository.findByRoomIdAndUsername(roomId, username)
                .orElseThrow(() -> new RuntimeException("User is not a member of this room"));

        roomMember.setLastReadAt(LocalDateTime.now());
        roomMemberRepository.save(roomMember);
    }

    public static class ReadReceipt {
        private Long messageId;
        private String username;
        private long timestamp;

        public ReadReceipt(Long messageId, String username, long timestamp) {
            this.messageId = messageId;
            this.username = username;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public Long getMessageId() {
            return messageId;
        }

        public void setMessageId(Long messageId) {
            this.messageId = messageId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
