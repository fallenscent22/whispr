package com.nikhitha.whispr.repository;

import com.nikhitha.whispr.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long>{
    Page<Message> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);
    
    List<Message> findTop50ByRoomIdOrderByCreatedAtDesc(String roomId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.roomId = :roomId AND m.isRead = false AND m.sender.username != :username")
    Long countUnreadMessages(@Param("roomId") String roomId, @Param("username") String username);
    
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isDelivered = true WHERE m.id IN :messageIds AND m.sender.username != :username")
    void markMessagesAsDelivered(@Param("messageIds") List<Long> messageIds, @Param("username") String username);
    
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.roomId = :roomId AND m.isRead = false AND m.sender.username != :username")
    void markMessagesAsRead(@Param("roomId") String roomId, @Param("username") String username);
}
