package com.nikhitha.whispr.service;

import com.nikhitha.whispr.dto.ChatRoomDTO;
import com.nikhitha.whispr.entity.ChatRoom;
import com.nikhitha.whispr.entity.RoomMember;
import com.nikhitha.whispr.entity.User;
import com.nikhitha.whispr.repository.ChatRoomRepository;
import com.nikhitha.whispr.repository.RoomMemberRepository;
import com.nikhitha.whispr.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public ChatRoom createChatRoom(ChatRoomDTO chatRoomDTO, String creatorUsername) {
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + creatorUsername));

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(chatRoomDTO.getName());
        chatRoom.setDescription(chatRoomDTO.getDescription());
        chatRoom.setType(chatRoomDTO.getType());
        chatRoom.setIsPrivate(chatRoomDTO.getIsPrivate());
        chatRoom.setMaxMembers(chatRoomDTO.getMaxMembers());
        chatRoom.setCreatedBy(creator);

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        // Add creator as owner
        addMemberToRoom(savedRoom, creator, RoomMember.MemberRole.OWNER);

        return savedRoom;
    }

    @Transactional
    public RoomMember addMemberToRoom(ChatRoom chatRoom, User user, RoomMember.MemberRole role) {
        // Check if user is already a member
        if (roomMemberRepository.findByChatRoomAndUser(chatRoom, user).isPresent()) {
            throw new RuntimeException("User is already a member of this room");
        }

        // Check room capacity
        Long memberCount = roomMemberRepository.countMembersByRoomId(chatRoom.getId());
        if (memberCount >= chatRoom.getMaxMembers()) {
            throw new RuntimeException("Room has reached maximum capacity");
        }

        RoomMember roomMember = new RoomMember();
        roomMember.setChatRoom(chatRoom);
        roomMember.setUser(user);
        roomMember.setRole(role);

        return roomMemberRepository.save(roomMember);
    }

    @Transactional
    public void removeMemberFromRoom(String roomId, String username, String removerUsername) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        User userToRemove = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        User remover = userRepository.findByUsername(removerUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + removerUsername));

        RoomMember removerMember = roomMemberRepository.findByChatRoomAndUser(chatRoom, remover)
                .orElseThrow(() -> new RuntimeException("Remover is not a member of this room"));

        RoomMember memberToRemove = roomMemberRepository.findByChatRoomAndUser(chatRoom, userToRemove)
                .orElseThrow(() -> new RuntimeException("User is not a member of this room"));

        if (!removerMember.getRole().equals(RoomMember.MemberRole.OWNER) &&
                !removerMember.getRole().equals(RoomMember.MemberRole.ADMIN)) {
            throw new RuntimeException("Insufficient permissions to remove members");
        }

        if (memberToRemove.getRole().equals(RoomMember.MemberRole.OWNER) &&
                !removerUsername.equals(username)) {
            throw new RuntimeException("Cannot remove room owner");
        }

        roomMemberRepository.delete(memberToRemove);
    }

    @Transactional(readOnly = true)
    public Page<ChatRoom> getUserChatRooms(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return chatRoomRepository.findRoomsByUserId(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<ChatRoom> discoverPublicRooms(Pageable pageable) {
        return chatRoomRepository.findByIsPrivateFalse(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ChatRoom> searchRooms(String searchTerm, Pageable pageable) {
        return chatRoomRepository.searchRooms(searchTerm, pageable);
    }

    @Transactional(readOnly = true)
    public ChatRoom getChatRoomByRoomId(String roomId) {
        return chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
    }

    @Transactional(readOnly = true)
    public List<User> getRoomMembers(String roomId) {
        ChatRoom chatRoom = getChatRoomByRoomId(roomId);
        return roomMemberRepository.findByChatRoom(chatRoom)
                .stream()
                .map(RoomMember::getUser)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateLastRead(String roomId, String username) {
        RoomMember roomMember = roomMemberRepository.findByRoomIdAndUsername(roomId, username)
                .orElseThrow(() -> new RuntimeException("User is not a member of this room"));

        roomMember.setLastReadAt(LocalDateTime.now());
        roomMemberRepository.save(roomMember);
    }

    @PostConstruct
    public void initializeGlobalRoom() {
        try {
            // Check if global room exists
            if (!chatRoomRepository.existsByRoomId("global")) {
                User systemUser = userRepository.findByUsername("system")
                        .orElseGet(() -> {
                            // Create a system user if doesn't exist
                            User user = new User();
                            user.setUsername("system");
                            user.setEmail("system@whispr.com");
                            user.setPassword(passwordEncoder.encode("system"));
                            return userRepository.save(user);
                        });

                ChatRoom globalRoom = new ChatRoom();
                globalRoom.setName("Global Chat");
                globalRoom.setDescription("Public global chat room for all users");
                globalRoom.setType(ChatRoom.RoomType.CHANNEL);
                globalRoom.setRoomId("global");
                globalRoom.setIsPrivate(false);
                globalRoom.setMaxMembers(10);
                globalRoom.setCreatedBy(systemUser);

                chatRoomRepository.save(globalRoom);
                System.out.println("Global chat room created successfully");
            }
        } catch (Exception e) {
            System.err.println("Failed to create global room: " + e.getMessage());
        }
    }

    @Transactional
    public ChatRoom updateChatRoom(String roomId, ChatRoomDTO chatRoomDTO, String updaterUsername) {
        ChatRoom chatRoom = getChatRoomByRoomId(roomId);

        RoomMember updater = roomMemberRepository.findByChatRoomAndUser(chatRoom,
                userRepository.findByUsername(updaterUsername)
                        .orElseThrow(() -> new RuntimeException("User not found")))
                .orElseThrow(() -> new RuntimeException("User is not a member of this room"));

        // Only owners and admins can update room details
        if (!updater.getRole().equals(RoomMember.MemberRole.OWNER) &&
                !updater.getRole().equals(RoomMember.MemberRole.ADMIN)) {
            throw new RuntimeException("Insufficient permissions to update room");
        }

        if (chatRoomDTO.getName() != null) {
            chatRoom.setName(chatRoomDTO.getName());
        }
        if (chatRoomDTO.getDescription() != null) {
            chatRoom.setDescription(chatRoomDTO.getDescription());
        }
        if (chatRoomDTO.getMaxMembers() != null) {
            chatRoom.setMaxMembers(chatRoomDTO.getMaxMembers());
        }

        return chatRoomRepository.save(chatRoom);
    }
}
