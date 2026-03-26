package com.openroof.openroof.repository;

import com.openroof.openroof.model.messaging.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :userId AND m.receiver.id = :otherId) OR " +
           "(m.sender.id = :otherId AND m.receiver.id = :userId) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversation(
            @Param("userId") Long userId,
            @Param("otherId") Long otherId);

    @Query("SELECT m FROM Message m WHERE m.sender.id = :userId OR m.receiver.id = :userId " +
           "ORDER BY m.createdAt DESC")
    List<Message> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.readAt IS NULL")
    long countUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.sender.id = :peerId AND m.readAt IS NULL")
    long countUnreadInConversation(@Param("userId") Long userId, @Param("peerId") Long peerId);

    @Query(value = "SELECT * FROM (" +
           "  SELECT DISTINCT ON (peer_id) m.*, " +
           "    CASE WHEN m.sender_id = :userId THEN m.receiver_id ELSE m.sender_id END AS peer_id " +
           "  FROM messages m " +
           "  WHERE (m.sender_id = :userId OR m.receiver_id = :userId) AND m.deleted_at IS NULL " +
           "  ORDER BY peer_id, m.created_at DESC" +
           ") sub ORDER BY created_at DESC",
           nativeQuery = true)
    List<Message> findLatestMessagePerConversation(@Param("userId") Long userId);
}
