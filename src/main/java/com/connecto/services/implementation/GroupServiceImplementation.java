package com.connecto.services.implementation;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.model.Group;
import com.connecto.model.Message;
import com.connecto.repositories.GroupMessageRepository;
import com.connecto.repositories.GroupRepository;
import com.connecto.repositories.UserRepository;
import com.connecto.services.GroupService;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class GroupServiceImplementation implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;

    public GroupServiceImplementation(
            GroupRepository groupRepository,
            GroupMessageRepository groupMessageRepository,
            UserRepository userRepository
    ) {
        this.groupRepository = groupRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Map<String, Object> getAllGroups(String userId) {
        try {
            DocumentReference userRef = userRepository.findUserReferenceById(userId);
            List<QueryDocumentSnapshot> docs = groupRepository.getAllGroups(userRef);
            List<Group> groups = new ArrayList<>();

            for (QueryDocumentSnapshot doc : docs) {
                groups.add(mapGroupDocument(doc));
            }

            return new HashMap<>() {{
                put("status", true);
                put("message", "Groups fetched successfully");
                put("data", groups);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
                put("data", Collections.emptyList());
            }};
        }
    }

    @Override
    public Map<String, Object> createGroup(String ownerId, String groupName, String groupAvatar, List<String> memberIds) {
        try {
            validateGroupName(groupName);

            List<String> safeMembers = memberIds == null ? new ArrayList<>() : memberIds;
            Set<String> unique = new LinkedHashSet<>(safeMembers);
            unique.remove(ownerId);
            if (unique.size() < 2) {
                throw new IllegalArgumentException("Group must contain at least 2 members");
            }
            if (unique.size() > 99) {
                throw new IllegalArgumentException("Group cannot contain more than 100 participants");
            }
            validateFriends(ownerId, unique);

            List<String> allMemberIds = new ArrayList<>(unique);
            allMemberIds.add(ownerId);

            List<DocumentReference> participantRefs = new ArrayList<>();
            Map<String, Long> unreadCounts = new HashMap<>();
            for (String memberId : allMemberIds) {
                DocumentSnapshot userDoc = userRepository.findUserById(memberId);
                if (!userDoc.exists()) {
                    throw new IllegalArgumentException("User not found: " + memberId);
                }
                participantRefs.add(userRepository.findUserReferenceById(memberId));
                unreadCounts.put(memberId, 0L);
            }

            DocumentReference groupRef = groupRepository.createGroup(
                    groupName.trim(),
                    groupAvatar,
                    userRepository.findUserReferenceById(ownerId),
                    participantRefs,
                    unreadCounts
            );

            Group group = mapGroupDocument(groupRepository.getGroupById(groupRef.getId()));
            return new HashMap<>() {{
                put("status", true);
                put("message", "Group created successfully");
                put("data", group);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
            }};
        }
    }

    @Override
    public Map<String, Object> getGroupMessages(String userId, String groupId, String lastVisible, Integer limit) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists()) {
                throw new IllegalArgumentException("Group not found");
            }
            if (!isParticipant(groupDoc, userId)) {
                throw new IllegalAccessException("You are not a participant of this group");
            }

            QuerySnapshot querySnapshot = groupMessageRepository.getMessages(groupId, lastVisible, limit);
            List<QueryDocumentSnapshot> docs = querySnapshot.getDocuments();

            List<Message> messages = docs.stream()
                    .map(doc -> doc.toObject(Message.class))
                    .filter(message -> message.getDeletedBy() == null || !message.getDeletedBy().contains(userId))
                    .collect(Collectors.toList());

            String cursor = docs.isEmpty() ? null : docs.get(docs.size() - 1).getId();

            return new HashMap<>() {{
                put("status", true);
                put("message", "Group messages fetched successfully");
                put("data", messages);
                put("lastVisible", cursor);
                put("hasMore", docs.size() == (limit == null || limit <= 0 ? 50 : Math.min(limit, 100)));
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
                put("data", Collections.emptyList());
            }};
        }
    }

    @Override
    public Map<String, Object> updateGroup(String userId, String groupId, String groupName, String groupAvatar) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists()) {
                throw new IllegalArgumentException("Group not found");
            }
            validateOwner(groupDoc, userId);
            if (groupName != null) {
                validateGroupName(groupName);
            }

            Map<String, Object> updates = new HashMap<>();
            if (groupName != null && !groupName.trim().isEmpty()) {
                updates.put("groupName", groupName.trim());
            }
            if (groupAvatar != null) {
                updates.put("groupAvatar", groupAvatar);
            }
            updates.put("updatedAt", new Date());

            groupRepository.updateGroup(groupId, updates);
            Group group = mapGroupDocument(groupRepository.getGroupById(groupId));
            return new HashMap<>() {{
                put("status", true);
                put("message", "Group updated successfully");
                put("data", group);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
            }};
        }
    }

    @Override
    public Map<String, Object> addMembers(String userId, String groupId, List<String> memberIds) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists()) {
                throw new IllegalArgumentException("Group not found");
            }
            validateOwner(groupDoc, userId);

            List<DocumentReference> participants = getParticipantRefs(groupDoc);
            Set<String> participantIds = participants.stream().map(DocumentReference::getId).collect(Collectors.toSet());
            Map<String, Long> unreadCounts = normalizeUnreadCounts(groupDoc.get("unreadCounts"));
            Set<String> requestedMembers = new LinkedHashSet<>(memberIds == null ? List.of() : memberIds);
            requestedMembers.removeAll(participantIds);
            validateFriends(userId, requestedMembers);
            if (participantIds.size() + requestedMembers.size() > 100) {
                throw new IllegalArgumentException("Group cannot contain more than 100 participants");
            }

            for (String memberId : requestedMembers) {
                if (!participantIds.contains(memberId)) {
                    DocumentSnapshot userDoc = userRepository.findUserById(memberId);
                    if (!userDoc.exists()) {
                        throw new IllegalArgumentException("User not found: " + memberId);
                    }
                    participants.add(userRepository.findUserReferenceById(memberId));
                    participantIds.add(memberId);
                    unreadCounts.put(memberId, 0L);
                }
            }

            groupRepository.updateGroup(groupId, new HashMap<>() {{
                put("participants", participants);
                put("unreadCounts", unreadCounts);
                put("updatedAt", new Date());
            }});

            Group group = mapGroupDocument(groupRepository.getGroupById(groupId));
            return new HashMap<>() {{
                put("status", true);
                put("message", "Members added successfully");
                put("data", group);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
            }};
        }
    }

    @Override
    public Map<String, Object> removeMembers(String userId, String groupId, List<String> memberIds) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists()) {
                throw new IllegalArgumentException("Group not found");
            }
            validateOwner(groupDoc, userId);

            String ownerId = groupDoc.getString("ownerId");
            Set<String> removeIds = new LinkedHashSet<>(memberIds == null ? new ArrayList<>() : memberIds);
            removeIds.remove(ownerId);

            List<DocumentReference> participants = getParticipantRefs(groupDoc)
                    .stream()
                    .filter(ref -> !removeIds.contains(ref.getId()))
                    .collect(Collectors.toList());

            if (participants.size() < 2) {
                throw new IllegalArgumentException("Group must contain at least 2 participants");
            }

            Map<String, Long> unreadCounts = normalizeUnreadCounts(groupDoc.get("unreadCounts"));
            removeIds.forEach(unreadCounts::remove);

            groupRepository.updateGroup(groupId, new HashMap<>() {{
                put("participants", participants);
                put("unreadCounts", unreadCounts);
                put("updatedAt", new Date());
            }});

            Group group = mapGroupDocument(groupRepository.getGroupById(groupId));
            return new HashMap<>() {{
                put("status", true);
                put("message", "Members removed successfully");
                put("data", group);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
            }};
        }
    }

    @Override
    public Map<String, Object> transferOwnership(String userId, String groupId, String newOwnerId) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists()) {
                throw new IllegalArgumentException("Group not found");
            }
            validateOwner(groupDoc, userId);
            if (!isParticipant(groupDoc, newOwnerId)) {
                throw new IllegalArgumentException("New owner must be a group participant");
            }

            groupRepository.updateGroup(groupId, new HashMap<>() {{
                put("ownerId", newOwnerId);
                put("updatedAt", new Date());
            }});

            Group group = mapGroupDocument(groupRepository.getGroupById(groupId));
            return new HashMap<>() {{
                put("status", true);
                put("message", "Ownership transferred successfully");
                put("data", group);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
            }};
        }
    }

    @Override
    public Map<String, Object> leaveGroup(String userId, String groupId) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists()) {
                throw new IllegalArgumentException("Group not found");
            }

            String ownerId = groupDoc.getString("ownerId");
            if (Objects.equals(ownerId, userId)) {
                throw new IllegalArgumentException("Owner must transfer ownership before leaving the group");
            }
            if (!isParticipant(groupDoc, userId)) {
                throw new IllegalAccessException("You are not a participant of this group");
            }

            List<DocumentReference> participants = getParticipantRefs(groupDoc)
                    .stream()
                    .filter(ref -> !Objects.equals(ref.getId(), userId))
                    .collect(Collectors.toList());

            Map<String, Long> unreadCounts = normalizeUnreadCounts(groupDoc.get("unreadCounts"));
            unreadCounts.remove(userId);

            groupRepository.updateGroup(groupId, new HashMap<>() {{
                put("participants", participants);
                put("unreadCounts", unreadCounts);
                put("updatedAt", new Date());
            }});

            Group group = mapGroupDocument(groupRepository.getGroupById(groupId));

            return new HashMap<>() {{
                put("status", true);
                put("message", "You left the group successfully");
                put("data", group);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
            }};
        }
    }

    @Override
    public Map<String, Object> addGroupMessage(String groupId, Message message) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists()) {
                throw new IllegalArgumentException("Group not found");
            }
            if (!isParticipant(groupDoc, message.getFrom())) {
                throw new IllegalAccessException("Sender is not a participant of this group");
            }

            groupRepository.addMessage(groupId, message);

            return new HashMap<>() {{
                put("status", true);
                put("message", "Message sent successfully");
                put("data", message);
                put("group_id", groupId);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
            }};
        }
    }

    @Override
    public void resetUnreadCount(String userId, String groupId) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists() || !isParticipant(groupDoc, userId)) {
                return;
            }
            groupRepository.resetUnreadCount(groupId, userId);
        } catch (Exception ignored) {
        }
    }

    @Override
    public Map<String, Object> deleteGroupMessage(String groupId, String messageId, String userId) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists()) {
                throw new IllegalArgumentException("Group not found");
            }
            if (!isParticipant(groupDoc, userId)) {
                throw new IllegalAccessException("You are not a participant of this group");
            }

            DocumentSnapshot messageDoc = groupMessageRepository.getMessageById(groupId, messageId);
            if (!messageDoc.exists()) {
                throw new IllegalArgumentException("Message not found");
            }

            groupMessageRepository.deleteForUser(groupId, messageId, userId);

            return new HashMap<>() {{
                put("status", true);
                put("message", "Message deleted successfully");
                put("group_id", groupId);
                put("message_id", messageId);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", e.getMessage());
                put("group_id", groupId);
                put("message_id", messageId);
            }};
        }
    }

    @Override
    public List<String> getParticipantIds(String groupId) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists()) {
                return new ArrayList<>();
            }
            return getParticipantRefs(groupDoc)
                    .stream()
                    .map(DocumentReference::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> deleteGroup(String userId, String groupId) {
        try {
            DocumentSnapshot groupDoc = groupRepository.getGroupById(groupId);
            if (!groupDoc.exists()) {
                throw new IllegalArgumentException("Group not found");
            }
            validateOwner(groupDoc, userId);
            groupRepository.deleteGroup(groupId);
            return Map.of("status", true, "message", "Group deleted successfully", "group_id", groupId);
        } catch (Exception e) {
            return Map.of("status", false, "message", e.getMessage(), "group_id", groupId);
        }
    }

    private void validateOwner(DocumentSnapshot groupDoc, String userId) throws IllegalAccessException {
        String ownerId = groupDoc.getString("ownerId");
        if (!Objects.equals(ownerId, userId)) {
            throw new IllegalAccessException("Only group owner can perform this action");
        }
    }

    private void validateGroupName(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required");
        }
        if (groupName.trim().length() > 80) {
            throw new IllegalArgumentException("Group name cannot exceed 80 characters");
        }
    }

    private void validateFriends(String ownerId, Set<String> memberIds) throws ExecutionException, InterruptedException {
        if (memberIds.isEmpty()) {
            return;
        }
        com.connecto.model.User owner = userRepository.getUserById(ownerId);
        Set<String> friendIds = owner == null || owner.getFriends() == null
                ? Set.of()
                : new HashSet<>(owner.getFriends());
        if (!friendIds.containsAll(memberIds)) {
            throw new IllegalArgumentException("Only friends can be added to a group");
        }
    }

    private boolean isParticipant(DocumentSnapshot groupDoc, String userId) {
        return getParticipantRefs(groupDoc)
                .stream()
                .map(DocumentReference::getId)
                .anyMatch(id -> Objects.equals(id, userId));
    }

    private List<DocumentReference> getParticipantRefs(DocumentSnapshot groupDoc) {
        List<DocumentReference> refs = (List<DocumentReference>) groupDoc.get("participants");
        return refs == null ? new ArrayList<>() : new ArrayList<>(refs);
    }

    private Map<String, Long> normalizeUnreadCounts(Object unreadObj) {
        Map<String, Long> normalized = new HashMap<>();
        if (!(unreadObj instanceof Map<?, ?> map)) {
            return normalized;
        }
        map.forEach((k, v) -> {
            if (k != null && v instanceof Number number) {
                normalized.put(k.toString(), number.longValue());
            }
        });
        return normalized;
    }

    private Group mapGroupDocument(DocumentSnapshot doc) throws ExecutionException, InterruptedException {
        List<UserResponseDTO> participants = new ArrayList<>();
        for (DocumentReference ref : getParticipantRefs(doc)) {
            DocumentSnapshot userDoc = ref.get().get();
            UserResponseDTO user = userDoc.toObject(UserResponseDTO.class);
            if (user != null) {
                participants.add(user);
            }
        }

        UserResponseDTO owner = null;
        String ownerId = doc.getString("ownerId");
        if (ownerId != null) {
            DocumentSnapshot ownerDoc = userRepository.findUserById(ownerId);
            owner = ownerDoc.toObject(UserResponseDTO.class);
        }

        return new Group()
                .setId(doc.getId())
                .setGroupName(doc.getString("groupName"))
                .setGroupAvatar(doc.getString("groupAvatar"))
                .setOwnerId(ownerId)
                .setOwner(owner)
                .setParticipants(participants)
                .setUnreadCounts(normalizeUnreadCounts(doc.get("unreadCounts")))
                .setLastMessage(doc.getString("lastMessage"))
                .setLastMessageTime(doc.getDate("lastMessageTime"))
                .setCreatedAt(doc.getDate("createdAt"))
                .setUpdatedAt(doc.getDate("updatedAt"));
    }
}
