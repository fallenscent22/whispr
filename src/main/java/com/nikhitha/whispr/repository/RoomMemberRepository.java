package com.nikhitha.whispr.repository;

import com.nikhitha.whispr.entity.RoomMember;
import com.nikhitha.whispr.entity.ChatRoom;
import com.nikhitha.whispr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long>{
    Optional<RoomMember> findByChatRoomAndUser(ChatRoom chatRoom, User user);
    
    List<RoomMember> findByChatRoom(ChatRoom chatRoom);
    
    List<RoomMember> findByUser(User user);
    
    @Query("SELECT COUNT(rm) FROM RoomMember rm WHERE rm.chatRoom.id = :roomId")
    Long countMembersByRoomId(@Param("roomId") Long roomId);
    
    @Query("SELECT rm FROM RoomMember rm WHERE rm.chatRoom.roomId = :roomId AND rm.user.username = :username")
    Optional<RoomMember> findByRoomIdAndUsername(@Param("roomId") String roomId, @Param("username") String username);

    @Query("SELECT rm FROM RoomMember rm WHERE rm.chatRoom.roomId = :roomId AND rm.user.id = :userId")
    Optional<RoomMember> findByRoomIdAndUserId(@Param("roomId") String roomId, @Param("userId") Long userId);
    
    void deleteByChatRoomAndUser(ChatRoom chatRoom, User user);
    
} 