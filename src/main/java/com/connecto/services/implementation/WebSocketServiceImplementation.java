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
            FriendRequest friendRequest = friendRequestRepository.getFriendRequestById(request.get("request_id").toString());

            //TODO Add a friends object in the senderFriends and recipientFriends

            userRepository.updateUser(friendRequest.getSender(), "friends", FieldValue.arrayUnion(friendRequest.getRecipient()));
            userRepository.updateUser(friendRequest.getRecipient(), "friends", FieldValue.arrayUnion(friendRequest.getSender()));
            userRepository.updateUser(friendRequest.getRecipient(), "friendRequests", FieldValue.arrayRemove(friendRequest.getId()));
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
}
