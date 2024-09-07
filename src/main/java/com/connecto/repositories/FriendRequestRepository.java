package com.connecto.repositories;

import com.connecto.model.FriendRequest;
import com.connecto.model.User;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public List<Map<String,Object>> getAllRequests(User user) throws ExecutionException, InterruptedException {
        List<String> friendRequests = user.getFriendRequests();
        if (friendRequests==null || friendRequests.isEmpty()){
            return new ArrayList<>();
        }
        List<Map<String,Object>> response = new ArrayList<>();
        friendRequests.forEach(el -> {
            try {
                FriendRequest requestDoc = friendRequestRef.document(el.toString()).get().get().toObject(FriendRequest.class);
                DocumentSnapshot sender = usersRef.document(requestDoc.getSender()).get().get();
                response.add(new HashMap<>(){{
                    put("id",requestDoc.getId());
                    put("sender_id",requestDoc.getSender());
                    put("firstName",sender.get("firstName"));
                    put("lastName",sender.get("lastName"));
                    put("avatar",sender.get("avatar"));
                    put("status",sender.get("status"));
                }});
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        return response;
    }

    public boolean deleteFriendRequest(String requestId) {
        return friendRequestRef.document(requestId).delete().isDone();
    }

    public Map<String, Object> addNewFriendRequest(FriendRequest friendRequest) {
        try {
            //Check if Friend request already exists
            QuerySnapshot existingRequest = friendRequestRef
                    .whereIn("sender", List.of(friendRequest.getSender(), friendRequest.getRecipient()))
                    .whereIn("recipient", List.of(friendRequest.getSender(), friendRequest.getRecipient()))
                    .get().get();
            if (!existingRequest.isEmpty()) {
                FriendRequest request = existingRequest.getDocuments().get(0).toObject(FriendRequest.class);
                if (request.getSender().equals(friendRequest.getSender())) {
                    return new HashMap<>() {{
                        put("status", "info");
                        put("message", "You have already sent this user a request");
                    }};
                } else {
                    return new HashMap<>() {{
                        put("status", "info");
                        put("message", "You have a pending request from this user");
                    }};
                }
            }
            //Create a new Friend Request
            DocumentReference friendRequestReference = friendRequestRef.document();
            friendRequest.setId(friendRequestReference.getId());
            friendRequestReference.set(friendRequest);
            //Update the user and add the new Request to their pending requests
            DocumentReference userReference = usersRef.document(friendRequest.getRecipient());
            userReference.update("friendRequests", FieldValue.arrayUnion(friendRequest.getId()));
            return new HashMap<>() {{
                put("status", "success");
                put("message", "Request sent successfully");
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", "error");
                put("message", "Something went wrong");
            }};
        }
    }

    public FriendRequest getFriendRequestById(String id) throws ExecutionException, InterruptedException {
        return friendRequestRef.document(id).get().get().toObject(FriendRequest.class);
    }
}
