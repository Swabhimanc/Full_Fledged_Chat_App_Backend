package com.connecto.repositories;

import com.connecto.model.Message;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
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
        List<QueryDocumentSnapshot> documents = oneToOneRef.whereEqualTo("participants", List.of(from,to))
                .get()
                .get()
                .getDocuments();
        return documents;
    }

    public WriteResult createDirectConversation(DocumentReference from, DocumentReference to) throws ExecutionException, InterruptedException {
        DocumentReference messageRef = oneToOneRef.document();
        return messageRef.set(new HashMap<>(){{
            put("id",messageRef.getId());
            put("participants",List.of(from,to));
            put("messages",List.of());
        }}).get();
    }

    public DocumentReference getConversationById(String id) {
        return oneToOneRef.document(id);
    }

    public void addMessageToConversation(String conversationId, Message message) {
        DocumentReference conversationRef = getConversationById(conversationId);
        conversationRef.update("messages",FieldValue.arrayUnion(message));
    }
}
