package com.nikhitha.whispr.repository;

import com.nikhitha.whispr.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
// import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>{
    Optional<ChatRoom> findByRoomId(String roomId);
    
    Page<ChatRoom> findByIsPrivateFalse(Pageable pageable);
    
    @Query("SELECT cr FROM ChatRoom cr JOIN RoomMember rm ON cr.id = rm.chatRoom.id WHERE rm.user.id = :userId")
    Page<ChatRoom> findRoomsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.name LIKE %:searchTerm% OR cr.description LIKE %:searchTerm%")
    Page<ChatRoom> searchRooms(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    boolean existsByRoomId(String roomId);
}
