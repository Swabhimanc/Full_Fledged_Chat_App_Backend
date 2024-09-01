package com.connecto.repositories;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class FriendRequestRepository {
    private final CollectionReference friendRequestRef;
    private final CollectionReference usersRef;

    public FriendRequestRepository(Firestore firestore) {
        this.friendRequestRef = firestore.collection("FriendRequestMaster");
        this.usersRef = firestore.collection("UsersMaster");
    }

    public CollectionReference getFriendRequestRef() {
        return this.friendRequestRef;
    }

    public List<QueryDocumentSnapshot> getAllRequests(String userId) throws ExecutionException, InterruptedException {
        return friendRequestRef.whereEqualTo("recipient", userId).get().get().getDocuments();
    }
}
