package com.connecto.services.implementation;

import com.connecto.model.Friend;
import com.connecto.model.FriendRequest;
import com.connecto.model.User;
import com.connecto.repositories.FriendRequestRepository;
import com.connecto.repositories.UserRepository;
import com.connecto.services.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
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
    public Map<String, Object> acceptFriendRequest(FriendRequest request) throws ExecutionException, InterruptedException {
        User sender = userRepository.findUserById(request.getSender()).toObject(User.class);
        User recipient = userRepository.findUserById(request.getRecipient()).toObject(User.class);
        List<Friend> senderFriends = sender.getFriends();
        List<Friend> recipientFriends = recipient.getFriends();
        userRepository.updateUser(sender.getId(),"friends",senderFriends);
        userRepository.updateUser(recipient.getId(),"friends",recipientFriends);
        if(friendRequestRepository.deleteFriendRequest(request.getId())){
            return new HashMap<>(){{
                put("status",true);
                put("message","Friend request accepted");
            }};
        }
        return new HashMap<>(){{
            put("status",false);
            put("message","Something went wrong");
        }};
    }
}
