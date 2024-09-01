package com.connecto.repositories;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.connecto.model.Message;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class MessageRepository {
    private final CollectionReference messageRef;

    public MessageRepository(Firestore firestore){
        this.messageRef = firestore.collection("MessageMaster");
    }
    public CollectionReference getMessageRef(){
        return messageRef;
    }

    public ApiFuture<DocumentReference> saveMessage(Message message) {
        return messageRef.add(message);
    }

    public QuerySnapshot getAllFromAndToMessages(String fromUser, String toUser) throws ExecutionException, InterruptedException {

        Query query = messageRef.whereIn("from", List.of(fromUser, toUser))
                .whereIn("to", List.of(fromUser, toUser))
                .orderBy("createdAt", Query.Direction.ASCENDING);
        QuerySnapshot querySnapshot = query.get().get();

        return querySnapshot;
    }
}
