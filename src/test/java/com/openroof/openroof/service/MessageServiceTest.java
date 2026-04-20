package com.openroof.openroof.service;

import com.openroof.openroof.dto.message.ConversationResponse;
import com.openroof.openroof.dto.message.MessageResponse;
import com.openroof.openroof.dto.message.SendMessageRequest;
import com.openroof.openroof.model.messaging.Message;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.MessageRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.openroof.openroof.dto.notification.CreateNotificationRequest;
import com.openroof.openroof.model.enums.NotificationType;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private User receiver;
    private String senderEmail = "sender@test.com";

    @BeforeEach
    void setUp() {
        sender = User.builder().name("Juan Perez").email(senderEmail).build();
        sender.setId(1L);
        receiver = User.builder().name("Maria Garcia").email("maria@test.com").build();
        receiver.setId(2L);
    }

    @Nested
    @DisplayName("getConversations()")
    class ConversationTests {
        @Test
        @DisplayName("Obtener conversaciones → mapea Peer e Iniciales")
        void getConversations_success() {
            Message m = Message.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .content("Hola mundo")
                    .build();

            when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(sender));
            when(messageRepository.findLatestMessagePerConversation(1L)).thenReturn(List.of(m));
            when(messageRepository.countUnreadInConversation(1L, 2L)).thenReturn(3L);

            List<ConversationResponse> result = messageService.getConversations(senderEmail);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).contactName()).isEqualTo("Maria Garcia");
            assertThat(result.get(0).unread()).isEqualTo(3);
            assertThat(result.get(0).lastMessageOwn()).isTrue(); // sender == current user
        }
    }

    @Nested
    @DisplayName("send()")
    class SendTests {
        @Test
        @DisplayName("Enviar mensaje → guarda en repo y retorna DTO")
        void sendMessage_success() {
            var request = new SendMessageRequest(2L, "Holi", null);

            when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(userRepository.findByEmail(receiver.getEmail())).thenReturn(Optional.of(receiver));
            when(messageRepository.hasRecentUnreadFromSender(
                    eq(2L),
                    eq(1L),
                    argThat(instant -> instant != null)
            )).thenReturn(false);

            Message saved = Message.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .content("Holi")
                    .build();
            saved.setId(100L);
            when(messageRepository.save(any())).thenReturn(saved);

            MessageResponse res = messageService.send(senderEmail, request);

            assertThat(res.id()).isEqualTo(100L);
            assertThat(res.text()).isEqualTo("Holi");
            assertThat(res.ownMessage()).isTrue();
            verify(messageRepository).save(any());
            verify(notificationService).create(
                    argThat(req ->
                        req != null
                        && req.type() == NotificationType.MESSAGE
                        && req.userId() != null
                    ),
                    eq(receiver.getEmail())
            );
        }

        @Test
        @DisplayName("Enviar mensaje cuando hay lectura reciente → sin notificación")
        void sendMessage_whenRecentlyRead_noNotification() {
            var request = new SendMessageRequest(2L, "Active", null);

            when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(userRepository.findByEmail(receiver.getEmail())).thenReturn(Optional.of(receiver));
            when(messageRepository.hasRecentUnreadFromSender(
                    eq(2L),
                    eq(1L),
                    argThat(instant -> instant != null)
            )).thenReturn(true);

            Message saved = Message.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .content("Active")
                    .build();
            saved.setId(101L);
            when(messageRepository.save(any())).thenReturn(saved);

            MessageResponse res = messageService.send(senderEmail, request);

            assertThat(res.id()).isEqualTo(101L);
            assertThat(res.text()).isEqualTo("Active");
            verify(messageRepository).save(any());
            verify(notificationService, never()).create(any(), any());
        }

        @Test
        @DisplayName("Enviar mensaje → correo solo despues del commit transaccional")
        void sendMessage_emailDeferredUntilCommit() {
            var request = new SendMessageRequest(2L, "Deferred", null);

            when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(userRepository.findByEmail(receiver.getEmail())).thenReturn(Optional.of(receiver));
            when(messageRepository.hasRecentUnreadFromSender(
                    eq(2L),
                    eq(1L),
                    argThat(instant -> instant != null)
            )).thenReturn(false);

            Message saved = Message.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .content("Deferred")
                    .build();
            saved.setId(102L);
            when(messageRepository.save(any())).thenReturn(saved);

            verify(emailService, never()).sendNewMessageEmailAsync(any(), any(), any());

            MessageResponse res = messageService.send(senderEmail, request);

            assertThat(res.id()).isEqualTo(102L);
            verify(emailService).sendNewMessageEmailAsync(
                    eq(receiver.getEmail()),
                    argThat(name -> name != null && !name.isEmpty()),
                    eq("Deferred")
            );
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkReadTests {
        @Test
        @DisplayName("Marcar como leído → guarda mensajes con readAt actualizado")
        void markAsRead_updatesMessages() {
            Message m = Message.builder()
                    .sender(receiver)
                    .receiver(sender)
                    .content("Hi")
                    .build();
            
            when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(sender));
            when(messageRepository.findConversation(1L, 2L)).thenReturn(List.of(m));

            messageService.markAsRead(senderEmail, 2L);

            assertThat(m.getReadAt()).isNotNull();
            verify(messageRepository).save(m);
        }
    }
}
