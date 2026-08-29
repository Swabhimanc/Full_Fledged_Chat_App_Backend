package com.connecto.repositories;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OneToOneMessageRepositoryTests {
    @Test
    void conversationIdIsStableRegardlessOfParticipantOrder() {
        String first = OneToOneMessageRepository.deterministicConversationId("user-a", "user-b");
        String reversed = OneToOneMessageRepository.deterministicConversationId("user-b", "user-a");

        assertEquals(first, reversed);
        assertEquals(47, first.length());
    }

    @Test
    void differentParticipantsProduceDifferentIds() {
        assertNotEquals(
                OneToOneMessageRepository.deterministicConversationId("user-a", "user-b"),
                OneToOneMessageRepository.deterministicConversationId("user-a", "user-c")
        );
    }
}
