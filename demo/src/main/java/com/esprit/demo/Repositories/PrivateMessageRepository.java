package com.esprit.demo.Repositories;

import com.esprit.demo.Models.PrivateMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    /** All messages between two users ordered chronologically */
    @Query("SELECT m FROM PrivateMessage m WHERE " +
           "(m.sender.id = :uid1 AND m.receiver.id = :uid2) OR " +
           "(m.sender.id = :uid2 AND m.receiver.id = :uid1) " +
           "ORDER BY m.createdAt ASC")
    List<PrivateMessage> findConversation(@Param("uid1") Long uid1, @Param("uid2") Long uid2);

    /** All messages involving a user (sent or received), newest first — used to build inbox */
    @Query("SELECT m FROM PrivateMessage m WHERE " +
           "m.sender.id = :userId OR m.receiver.id = :userId " +
           "ORDER BY m.createdAt DESC")
    List<PrivateMessage> findAllForUser(@Param("userId") Long userId);

    /** Unread messages received by userId from a specific sender */
    @Query("SELECT COUNT(m) FROM PrivateMessage m WHERE " +
           "m.sender.id = :senderId AND m.receiver.id = :receiverId AND m.read = false")
    long countUnreadFromSender(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    /** Total unread messages for a user across all conversations */
    @Query("SELECT COUNT(m) FROM PrivateMessage m WHERE m.receiver.id = :userId AND m.read = false")
    long countTotalUnread(@Param("userId") Long userId);

    /** Mark all messages from sender → receiver as read */
    @Modifying
    @Query("UPDATE PrivateMessage m SET m.read = true WHERE " +
           "m.sender.id = :senderId AND m.receiver.id = :receiverId AND m.read = false")
    void markConversationRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
