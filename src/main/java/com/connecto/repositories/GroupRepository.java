package com.connecto.repositories;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.connecto.model.Message;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Repository
public class GroupRepository {

    private final Firestore firestore;
    private final CollectionReference groupRef;

    public GroupRepository(Firestore firestore) {
        this.firestore = firestore;
        this.groupRef = firestore.collection("GroupMaster");
    }

    public DocumentReference createGroup(
            String groupName,
            String groupAvatar,
            DocumentReference ownerRef,
            List<DocumentReference> participants,
            Map<String, Long> unreadCounts
    ) throws ExecutionException, InterruptedException {
        DocumentReference docRef = groupRef.document();
        Date now = new Date();

        docRef.set(new HashMap<>() {{
            put("id", docRef.getId());
            put("groupName", groupName);
            put("groupAvatar", groupAvatar);
            put("ownerId", ownerRef.getId());
            put("participants", participants);
            put("unreadCounts", unreadCounts);
            put("lastMessage", "");
            put("lastMessageTime", null);
            put("createdAt", now);
            put("updatedAt", now);
        }}).get();

        return docRef;
    }

    public List<QueryDocumentSnapshot> getAllGroups(DocumentReference userRef) throws ExecutionException, InterruptedException {
        return groupRef.whereArrayContains("participants", userRef).get().get().getDocuments();
    }

    public DocumentReference getGroupReference(String groupId) {
        return groupRef.document(groupId);
    }

    public DocumentSnapshot getGroupById(String groupId) throws ExecutionException, InterruptedException {
        return groupRef.document(groupId).get().get();
    }

    public void updateGroup(String groupId, Map<String, Object> updates) throws ExecutionException, InterruptedException {
        groupRef.document(groupId).update(updates).get();
    }

    public CollectionReference getMessageCollection(String groupId) {
        return groupRef.document(groupId).collection("messages");
    }

    public void addMessage(String groupId, Message message) throws Exception {
        DocumentReference group = groupRef.document(groupId);
        DocumentReference messageRef = getMessageCollection(groupId).document(message.getId());
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(group).get();
            if (!snapshot.exists()) {
                throw new IllegalArgumentException("Group not found");
            }
            Map<String, Long> unreadCounts = normalizeCounts(snapshot.get("unreadCounts"));
            List<DocumentReference> participants = (List<DocumentReference>) snapshot.get("participants");
            if (participants != null) {
                for (DocumentReference participant : participants) {
                    String id = participant.getId();
                    unreadCounts.put(id, id.equals(message.getFrom()) ? 0L : unreadCounts.getOrDefault(id, 0L) + 1);
                }
            }
            String lastMessage = message.getText() == null || message.getText().isBlank()
                    ? (message.getMediaType() == null ? "" : message.getMediaType())
                    : message.getText();
            transaction.set(messageRef, message);
            transaction.update(group, Map.of(
                    "unreadCounts", unreadCounts,
                    "lastMessage", lastMessage,
                    "lastMessageTime", message.getCreatedAt(),
                    "updatedAt", new Date()
            ));
            return null;
        }).get();
    }

    public void resetUnreadCount(String groupId, String userId) throws ExecutionException, InterruptedException {
        groupRef.document(groupId).update("unreadCounts." + userId, 0L, "updatedAt", new Date()).get();
    }

    public void deleteGroup(String groupId) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> messages;
        do {
            messages = getMessageCollection(groupId).limit(400).get().get().getDocuments();
            if (!messages.isEmpty()) {
                WriteBatch batch = firestore.batch();
                messages.forEach(message -> batch.delete(message.getReference()));
                batch.commit().get();
            }
        } while (!messages.isEmpty());
        groupRef.document(groupId).delete().get();
    }

    private Map<String, Long> normalizeCounts(Object value) {
        Map<String, Long> counts = new HashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, count) -> {
                if (key != null && count instanceof Number number) {
                    counts.put(key.toString(), number.longValue());
                }
            });
        }
        return counts;
    }
}
