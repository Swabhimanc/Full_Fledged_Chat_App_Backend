package com.connecto.services.implementation;

import com.connecto.repositories.MessageRepository;
import com.connecto.repositories.OneToOneMessageRepository;
import com.connecto.repositories.UserRepository;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageServiceAuthorizationTests {
    @Test
    void rejectsMessageReadsForNonParticipants() throws Exception {
        MessageRepository messageRepository = mock(MessageRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        OneToOneMessageRepository conversationRepository = mock(OneToOneMessageRepository.class);
        DocumentReference conversationRef = mock(DocumentReference.class);
        DocumentReference participantRef = mock(DocumentReference.class);
        DocumentSnapshot conversation = mock(DocumentSnapshot.class);

        when(conversationRepository.getConversationById("room-1")).thenReturn(conversationRef);
        when(conversationRef.get()).thenReturn(ApiFutures.immediateFuture(conversation));
        when(conversation.exists()).thenReturn(true);
        when(conversation.get("participants")).thenReturn(List.of(participantRef));
        when(participantRef.getId()).thenReturn("another-user");

        MessageServiceImplementation service = new MessageServiceImplementation(
                messageRepository,
                userRepository,
                conversationRepository
        );

        assertThrows(AccessDeniedException.class, () -> service.getOneToOneMessages("room-1", "attacker"));
    }
}
