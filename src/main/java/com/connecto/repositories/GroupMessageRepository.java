package com.connecto.repositories;

import com.connecto.model.Message;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

@Repository
public class GroupMessageRepository {

    private final Firestore firestore;

    public GroupMessageRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference messageRef(String groupId) {
        return firestore.collection("GroupMaster").document(groupId).collection("messages");
    }

    public void addMessage(String groupId, Message message) throws ExecutionException, InterruptedException {
        messageRef(groupId).document(message.getId()).set(message).get();
    }

    public DocumentSnapshot getMessageById(String groupId, String messageId) throws ExecutionException, InterruptedException {
        return messageRef(groupId).document(messageId).get().get();
    }

    public void updateMessage(String groupId, String messageId, String field, Object value) throws ExecutionException, InterruptedException {
        messageRef(groupId).document(messageId).update(field, value).get();
    }

    public void deleteForUser(String groupId, String messageId, String userId) throws ExecutionException, InterruptedException {
        messageRef(groupId).document(messageId).update("deletedBy", FieldValue.arrayUnion(userId)).get();
    }

    public QuerySnapshot getMessages(String groupId, String lastVisible, Integer limit) throws ExecutionException, InterruptedException {
        int pageSize = limit == null || limit <= 0 ? 50 : Math.min(limit, 100);
        Query query = messageRef(groupId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize);

        if (lastVisible != null && !lastVisible.isBlank()) {
            DocumentSnapshot cursor = messageRef(groupId).document(lastVisible).get().get();
            if (cursor.exists()) {
                query = query.startAfter(cursor);
            }
        }
        return query.get().get();
    }
}
