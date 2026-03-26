package com.openroof.openroof.service;

import com.openroof.openroof.dto.message.ConversationResponse;
import com.openroof.openroof.dto.message.MessageResponse;
import com.openroof.openroof.dto.message.SendMessageRequest;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.messaging.Message;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.MessageRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    public List<ConversationResponse> getConversations(String email) {
        User user = findUserByEmail(email);
        Long userId = user.getId();

        List<Message> latestMessages = messageRepository.findLatestMessagePerConversation(userId);

        return latestMessages.stream().map(m -> {
            User peer = m.getSender().getId().equals(userId) ? m.getReceiver() : m.getSender();
            String name = peer.getName() != null ? peer.getName() : peer.getEmail();
            String initials = buildInitials(name);

            // Count unread in this conversation
            long unread = messageRepository.findConversation(userId, peer.getId()).stream()
                    .filter(msg -> msg.getReceiver().getId().equals(userId) && msg.getReadAt() == null)
                    .count();

            return new ConversationResponse(
                    peer.getId(),
                    name,
                    m.getContent().length() > 80 ? m.getContent().substring(0, 80) + "..." : m.getContent(),
                    m.getCreatedAt(),
                    (int) unread,
                    initials
            );
        }).collect(Collectors.toList());
    }

    public List<MessageResponse> getMessages(String email, Long peerId) {
        User user = findUserByEmail(email);

        return messageRepository.findConversation(user.getId(), peerId).stream()
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getContent(),
                        m.getSender().getId().equals(user.getId()) ? "agent" : "client",
                        m.getCreatedAt(),
                        m.getSender().getId(),
                        m.getReceiver().getId()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse send(String email, SendMessageRequest request) {
        User sender = findUserByEmail(email);
        User receiver = userRepository.findById(request.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinatario no encontrado"));

        Message.MessageBuilder builder = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.content());

        if (request.propertyId() != null) {
            Property property = propertyRepository.findById(request.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));
            builder.property(property);
        }

        Message saved = messageRepository.save(builder.build());

        return new MessageResponse(
                saved.getId(),
                saved.getContent(),
                "agent",
                saved.getCreatedAt(),
                sender.getId(),
                receiver.getId()
        );
    }

    @Transactional
    public void markAsRead(String email, Long peerId) {
        User user = findUserByEmail(email);
        messageRepository.findConversation(user.getId(), peerId).stream()
                .filter(m -> m.getReceiver().getId().equals(user.getId()) && m.getReadAt() == null)
                .forEach(m -> {
                    m.setReadAt(LocalDateTime.now());
                    messageRepository.save(m);
                });
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private String buildInitials(String name) {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
