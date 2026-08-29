package com.connecto.repositories;

import com.connecto.model.Message;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Repository
public class OneToOneMessageRepository {
    private static final int SCHEMA_VERSION = 2;
    private static final int MIGRATION_BATCH_SIZE = 400;

    private final Firestore firestore;
    private final CollectionReference oneToOneRef;

    public OneToOneMessageRepository(Firestore firestore) {
        this.firestore = firestore;
        this.oneToOneRef = firestore.collection("OneToOneMessageMaster");
    }

    public List<QueryDocumentSnapshot> getAllDirectConversations(DocumentReference userRef) throws ExecutionException, InterruptedException {
        return oneToOneRef.whereArrayContains("participants", userRef).get().get().getDocuments();
    }

    public DocumentSnapshot getOrCreateDirectConversation(DocumentReference from, DocumentReference to) throws Exception {
        List<QueryDocumentSnapshot> existing = getDirectConversation(from, to);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        DocumentReference conversation = oneToOneRef.document(deterministicConversationId(from.getId(), to.getId()));
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(conversation).get();
            if (!snapshot.exists()) {
                transaction.set(conversation, Map.of(
                        "id", conversation.getId(),
                        "participants", List.of(from, to),
                        "createdAt", new Date(),
                        "updatedAt", new Date(),
                        "schemaVersion", SCHEMA_VERSION,
                        "unreadCounts", Map.of(from.getId(), 0L, to.getId(), 0L)
                ));
            }
            return null;
        }).get();
        return conversation.get().get();
    }

    public List<QueryDocumentSnapshot> getDirectConversation(DocumentReference from, DocumentReference to) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> direct = oneToOneRef.whereEqualTo("participants", List.of(from, to)).get().get().getDocuments();
        if (!direct.isEmpty()) {
            return direct;
        }
        return oneToOneRef.whereEqualTo("participants", List.of(to, from)).get().get().getDocuments();
    }

    public DocumentReference getConversationById(String id) {
        return oneToOneRef.document(id);
    }

    public void addMessageToConversation(String conversationId, Message message) throws Exception {
        DocumentReference conversation = getConversationById(conversationId);
        ensureMigrated(conversation.get().get());
        DocumentReference messageRef = messages(conversationId).document(message.getId());

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(conversation).get();
            if (!snapshot.exists()) {
                throw new IllegalArgumentException("Conversation not found");
            }
            Map<String, Long> unreadCounts = unreadCounts(snapshot.get("unreadCounts"));
            unreadCounts.put(message.getFrom(), 0L);
            unreadCounts.put(message.getTo(), unreadCounts.getOrDefault(message.getTo(), 0L) + 1);

            transaction.set(messageRef, message);
            String lastMessage = message.getText() == null || message.getText().isBlank()
                    ? (message.getMediaType() == null ? "" : message.getMediaType())
                    : message.getText();
            transaction.update(conversation, Map.of(
                    "unreadCounts", unreadCounts,
                    "lastMessage", lastMessage,
                    "lastMessageTime", message.getCreatedAt(),
                    "updatedAt", new Date(),
                    "schemaVersion", SCHEMA_VERSION
            ));
            return null;
        }).get();
    }

    public List<Message> getMessages(String conversationId, int limit, String userId) throws Exception {
        DocumentSnapshot conversation = getConversationById(conversationId).get().get();
        ensureMigrated(conversation);
        Query query = messages(conversationId).orderBy("createdAt", Query.Direction.DESCENDING).limit(Math.max(1, Math.min(limit, 100)));

        Timestamp hiddenBefore = hiddenBefore(conversation, userId);
        if (hiddenBefore != null) {
            query = query.whereGreaterThan("createdAt", hiddenBefore);
        }

        List<Message> result = new ArrayList<>();
        for (QueryDocumentSnapshot document : query.get().get().getDocuments()) {
            Message message = document.toObject(Message.class);
            if (message.getDeletedBy() == null || !message.getDeletedBy().contains(userId)) {
                result.add(message);
            }
        }
        java.util.Collections.reverse(result);
        return result;
    }

    public void resetUnreadCount(String conversationId, String userId) throws ExecutionException, InterruptedException {
        getConversationById(conversationId).update("unreadCounts." + userId, 0L, "updatedAt", new Date()).get();
    }

    public void deleteMessageForUser(String conversationId, String messageId, String userId) throws Exception {
        DocumentSnapshot conversation = getConversationById(conversationId).get().get();
        ensureMigrated(conversation);
        DocumentReference message = messages(conversationId).document(messageId);
        if (!message.get().get().exists()) {
            throw new IllegalArgumentException("Message not found");
        }
        message.update("deletedBy", FieldValue.arrayUnion(userId)).get();
    }

    public void hideChat(String conversationId, String userId) throws ExecutionException, InterruptedException {
        getConversationById(conversationId).update(Map.of(
                "hiddenBefore." + userId, Timestamp.now(),
                "unreadCounts." + userId, 0L,
                "updatedAt", new Date()
        )).get();
    }

    public void ensureMigrated(DocumentSnapshot conversation) throws ExecutionException, InterruptedException {
        if (!conversation.exists() || (conversation.getLong("schemaVersion") != null && conversation.getLong("schemaVersion") >= SCHEMA_VERSION)) {
            return;
        }

        List<Map<String, Object>> legacyMessages = (List<Map<String, Object>>) conversation.get("messages");
        if (legacyMessages == null) {
            conversation.getReference().update("schemaVersion", SCHEMA_VERSION).get();
            return;
        }

        WriteBatch batch = firestore.batch();
        int writes = 0;
        for (Map<String, Object> legacyMessage : legacyMessages) {
            String messageId = String.valueOf(legacyMessage.get("id"));
            if (messageId == null || "null".equals(messageId)) {
                continue;
            }
            batch.set(messages(conversation.getId()).document(messageId), legacyMessage);
            writes++;
            if (writes % MIGRATION_BATCH_SIZE == 0) {
                batch.commit().get();
                batch = firestore.batch();
            }
        }
        if (writes % MIGRATION_BATCH_SIZE != 0) {
            batch.commit().get();
        }
        conversation.getReference().update(Map.of(
                "schemaVersion", SCHEMA_VERSION,
                "legacyMessageCount", legacyMessages.size(),
                "messages", FieldValue.delete(),
                "migratedAt", Timestamp.now()
        )).get();
    }

    public int migrateAllConversations() throws ExecutionException, InterruptedException {
        int migrated = 0;
        for (QueryDocumentSnapshot conversation : oneToOneRef.get().get().getDocuments()) {
            Long version = conversation.getLong("schemaVersion");
            if (version == null || version < SCHEMA_VERSION) {
                ensureMigrated(conversation);
                migrated++;
            }
        }
        return migrated;
    }

    public static String deterministicConversationId(String firstUserId, String secondUserId) {
        List<String> sorted = new ArrayList<>(List.of(firstUserId, secondUserId));
        sorted.sort(String::compareTo);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.join(":", sorted).getBytes(StandardCharsets.UTF_8));
            return "direct_" + HexFormat.of().formatHex(digest).substring(0, 40);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create conversation id", e);
        }
    }

    private CollectionReference messages(String conversationId) {
        return oneToOneRef.document(conversationId).collection("messages");
    }

    private Timestamp hiddenBefore(DocumentSnapshot conversation, String userId) {
        Object hidden = conversation.get("hiddenBefore");
        if (hidden instanceof Map<?, ?> map && map.get(userId) instanceof Timestamp timestamp) {
            return timestamp;
        }
        return null;
    }

    private Map<String, Long> unreadCounts(Object value) {
        Map<String, Long> result = new HashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, count) -> {
                if (key != null && count instanceof Number number) {
                    result.put(key.toString(), number.longValue());
                }
            });
        }
        return result;
    }
}
