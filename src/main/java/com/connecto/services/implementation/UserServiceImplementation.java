package com.connecto.services.implementation;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.model.FriendRequest;
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
            if (!doc.getId().equals(user.getId()) && !user.getFriends().contains(doc.getId())) {
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
            List<QueryDocumentSnapshot> result = friendRequestRepository.getAllRequests(user.getId());
            List<FriendRequest> response = new ArrayList<>();
            result.forEach(doc -> {
                response.add(doc.toObject(FriendRequest.class));
            });
            return new HashMap<>() {{
                put("status", true);
                put("message", "Friend Requests fetched successfully");
                put("data", response);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Something went wrong while getting friend requests");
            }};
        }
    }
}
