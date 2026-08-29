package com.connecto.services.implementation;

import com.connecto.model.Message;
import com.connecto.model.User;
import com.connecto.repositories.GroupMessageRepository;
import com.connecto.repositories.GroupRepository;
import com.connecto.repositories.UserRepository;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupServiceAuthorizationTests {
    private GroupRepository groupRepository;
    private GroupMessageRepository messageRepository;
    private UserRepository userRepository;
    private GroupServiceImplementation service;

    @BeforeEach
    void setUp() {
        groupRepository = mock(GroupRepository.class);
        messageRepository = mock(GroupMessageRepository.class);
        userRepository = mock(UserRepository.class);
        service = new GroupServiceImplementation(groupRepository, messageRepository, userRepository);
    }

    @Test
    void rejectsCreatingGroupWithNonFriends() throws Exception {
        User owner = new User();
        owner.setFriends(List.of("friend-1"));
        when(userRepository.getUserById("owner")).thenReturn(owner);

        Map<String, Object> response = service.createGroup(
                "owner", "Project room", "", List.of("friend-1", "stranger")
        );

        assertFalse((boolean) response.get("status"));
        verify(groupRepository, never()).createGroup(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyMap()
        );
    }

    @Test
    void rejectsMessageFromNonParticipant() throws Exception {
        DocumentSnapshot group = groupWithOwnerAndParticipants("owner", List.of("member"));
        when(groupRepository.getGroupById("group-1")).thenReturn(group);
        Message message = new Message().setFrom("attacker");

        Map<String, Object> response = service.addGroupMessage("group-1", message);

        assertFalse((boolean) response.get("status"));
        verify(groupRepository, never()).addMessage("group-1", message);
    }

    @Test
    void rejectsGroupDeletionByNonOwner() throws Exception {
        DocumentSnapshot group = groupWithOwnerAndParticipants("owner", List.of("member"));
        when(groupRepository.getGroupById("group-1")).thenReturn(group);

        Map<String, Object> response = service.deleteGroup("member", "group-1");

        assertFalse((boolean) response.get("status"));
        verify(groupRepository, never()).deleteGroup("group-1");
    }

    @Test
    void allowsOwnerToDeleteGroup() throws Exception {
        DocumentSnapshot group = groupWithOwnerAndParticipants("owner", List.of("owner", "member"));
        when(groupRepository.getGroupById("group-1")).thenReturn(group);

        Map<String, Object> response = service.deleteGroup("owner", "group-1");

        assertTrue((boolean) response.get("status"));
        verify(groupRepository).deleteGroup("group-1");
    }

    private DocumentSnapshot groupWithOwnerAndParticipants(String ownerId, List<String> participantIds) {
        DocumentSnapshot group = mock(DocumentSnapshot.class);
        List<DocumentReference> participants = participantIds.stream().map(id -> {
            DocumentReference reference = mock(DocumentReference.class);
            when(reference.getId()).thenReturn(id);
            return reference;
        }).toList();
        when(group.exists()).thenReturn(true);
        when(group.getString("ownerId")).thenReturn(ownerId);
        when(group.get("participants")).thenReturn(participants);
        return group;
    }
}
