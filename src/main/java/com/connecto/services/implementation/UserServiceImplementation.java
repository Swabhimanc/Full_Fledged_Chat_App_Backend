package com.connecto.services.implementation;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.enums.Status;
import com.connecto.model.Friend;
import com.connecto.model.User;
import com.connecto.repositories.FriendRequestRepository;
import com.connecto.repositories.UserRepository;
import com.connecto.services.UserService;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserServiceImplementation implements UserService {

    private UserRepository userRepository;
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    public UserServiceImplementation(UserRepository userRepository, FriendRequestRepository friendRequestRepository) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    @Override
    public Map<String, Object> getAllUsers(User user) throws ExecutionException, InterruptedException {
        List<UserResponseDTO> response = new ArrayList<>();
        List<QueryDocumentSnapshot> usersSnapshot = userRepository.getAllUsers();
        usersSnapshot.forEach(doc -> {
            if (user.getFriends() == null || !user.getFriends().contains(doc.getId()) && !doc.getId().equals(user.getId())) {
                UserResponseDTO userResponseDTO = doc.toObject(UserResponseDTO.class);
                response.add(userResponseDTO);
            }
        });
        return new HashMap<>() {{
            put("status", true);
            put("message", "Users fetched successfully");
            put("data", response);
        }};
    }

    @Override
    public Map<String, Object> getFriendRequests(User user) {
        try {
            List<Map<String, Object>> result = friendRequestRepository.getAllRequests(user);
            if (!result.isEmpty()) {
                return new HashMap<>() {{
                    put("status", true);
                    put("message", "Friend Requests fetched successfully");
                    put("data", result);
                }};
            } else {
                return new HashMap<>() {{
                    put("status", true);
                    put("message", "You have no new friend requests");
                }};
            }
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Something went wrong while getting friend requests");
            }};
        }
    }

    public void setUserStatus(String userId, Status status) throws ExecutionException, InterruptedException {
        userRepository.updateUser(userId, "status", status);
    }

    @Override
    public Map<String, Object> getFriends(User user) throws ExecutionException, InterruptedException {
        List<String> friends = user.getFriends();
        List<Friend> response = new ArrayList<>();
        if(friends!=null){
            for(String id : friends){
                response.add(userRepository.findUserById(id).toObject(Friend.class));
            }
            return new HashMap<>(){{
                put("status",true);
                put("data",response);
            }};
        }
        return new HashMap<>(){{
            put("status",false);
            put("data",new ArrayList<>());
        }};
    }
}
