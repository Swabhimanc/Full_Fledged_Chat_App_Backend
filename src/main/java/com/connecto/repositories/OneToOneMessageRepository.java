package com.connecto.repositories;

import com.connecto.model.Message;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


@Repository
public class OneToOneMessageRepository {
    private final CollectionReference oneToOneRef;


    public OneToOneMessageRepository(Firestore firestore) {
        this.oneToOneRef = firestore.collection("OneToOneMessageMaster");
    }

    public List<QueryDocumentSnapshot> getAllDirectConversations(DocumentReference userRef) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> query = oneToOneRef.whereArrayContains("participants", userRef).get().get().getDocuments();
        return query;
    }

    public List<QueryDocumentSnapshot> getDirectConversation(DocumentReference from, DocumentReference to) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> document1 = oneToOneRef
                .whereEqualTo("participants", List.of(from, to))
                .get()
                .get()
                .getDocuments();
        List<QueryDocumentSnapshot> document2 = oneToOneRef
                .whereEqualTo("participants", List.of(to, from))
                .get()
                .get()
                .getDocuments();
        if (document1.isEmpty()) {
            return document2; //or document1
        } else {
            return document1;
        }
    }

    public WriteResult createDirectConversation(DocumentReference from, DocumentReference to) throws ExecutionException, InterruptedException {
        DocumentReference messageRef = oneToOneRef.document();
        return messageRef.set(new HashMap<>() {{
            put("id", messageRef.getId());
            put("participants", List.of(from, to));
            put("messages", List.of());
            put("unreadCounts", new HashMap<>(){{
                put(from.getId(),0);
                put(to.getId(),0);
            }});
        }}).get();
    }

    public DocumentReference getConversationById(String id) {
        return oneToOneRef.document(id);
    }

    public void addMessageToConversation(String conversationId, Message message) throws ExecutionException, InterruptedException {
        DocumentReference conversationRef = getConversationById(conversationId);
        conversationRef.update("messages", FieldValue.arrayUnion(message));
        Map<String,Long> unreadCounts = (Map<String, Long>) conversationRef.get().get().get("unreadCounts");
        if(unreadCounts!=null){
            Long oldCount = unreadCounts.get(message.getTo());
            unreadCounts.put(message.getFrom(), 0L);
            unreadCounts.put(message.getTo(), ++oldCount);
        }else {
            unreadCounts = new HashMap<>(){{
                put(message.getFrom(), 0L);
                put(message.getTo(), 1L);
            }};
        }
        conversationRef.update("unreadCounts",unreadCounts);
    }
}
