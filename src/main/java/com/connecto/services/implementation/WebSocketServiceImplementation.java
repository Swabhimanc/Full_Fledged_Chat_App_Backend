package com.connecto.services.implementation;

import com.connecto.model.FriendRequest;
import com.connecto.repositories.FriendRequestRepository;
import com.connecto.repositories.UserRepository;
import com.connecto.services.WebSocketService;
import com.google.cloud.firestore.FieldValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class WebSocketServiceImplementation implements WebSocketService {

    private UserRepository userRepository;
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    public WebSocketServiceImplementation(UserRepository userRepository, FriendRequestRepository friendRequestRepository) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    @Override
    public Map<String, Object> acceptFriendRequest(Map<String, Object> request) throws ExecutionException, InterruptedException {
        try {
            String sender = request.get("sender_id").toString();
            String recipient = request.get("recipient_id").toString();
            FriendRequest friendRequest = friendRequestRepository.getFriendRequestBySenderAndRecipient(sender,recipient);

            userRepository.updateUser(friendRequest.getSender(), "friends", FieldValue.arrayUnion(friendRequest.getRecipient()));
            userRepository.updateUser(friendRequest.getRecipient(), "friends", FieldValue.arrayUnion(friendRequest.getSender()));
            userRepository.updateUser(friendRequest.getRecipient(), "friendRequestsReceived", FieldValue.arrayRemove(friendRequest.getSender()));
            userRepository.updateUser(friendRequest.getSender(), "friendRequestsSent", FieldValue.arrayRemove(friendRequest.getRecipient()));
            friendRequestRepository.deleteFriendRequest(friendRequest.getId());

            return new HashMap<>() {{
                put("status", true);
                put("message", "Accepted Friend Request");
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Something went wrong");
            }};
        }
    }

    @Override
    public Map<String, Object> newFriendRequest(String from, String to) throws ExecutionException, InterruptedException {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(from);
        friendRequest.setRecipient(to);
        return friendRequestRepository.addNewFriendRequest(friendRequest);
    }

    @Override
    public Map<String, Object> deleteFriendRequest(String from, String to) throws ExecutionException, InterruptedException {
        FriendRequest friendRequest = friendRequestRepository.getFriendRequestBySenderAndRecipient(from,to);
        if(friendRequest==null){
            return new HashMap<>(){{
               put("status",false);
               put("message","No Requests Found");
            }};
        }
        userRepository.updateUser(friendRequest.getRecipient(), "friendRequestsReceived", FieldValue.arrayRemove(friendRequest.getSender()));
        userRepository.updateUser(friendRequest.getSender(), "friendRequestsSent", FieldValue.arrayRemove(friendRequest.getRecipient()));
        friendRequestRepository.deleteFriendRequest(friendRequest.getId());
        return new HashMap<>(){{
            put("status",true);
            put("message","Friend Request Cancelled");
        }};
    }

    @Override
    public Map<String, Object> removeFriend(Map<String, Object> request) throws ExecutionException, InterruptedException {
        try{
            String user_id = request.get("from").toString();
            String friend_id = request.get("to").toString();
            userRepository.updateUser(user_id,"friends",FieldValue.arrayRemove(friend_id));
            userRepository.updateUser(friend_id,"friends",FieldValue.arrayRemove(user_id));
            return new HashMap<>(){{
                put("status",true);
                put("message","Friend Removed Successfully");
            }};
        } catch (Exception e) {
            return new HashMap<>(){{
                put("status",true);
                put("message",e.getMessage());
            }};
        }
    }
}
