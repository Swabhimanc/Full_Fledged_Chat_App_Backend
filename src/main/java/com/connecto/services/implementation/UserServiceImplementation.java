package com.connecto.services.implementation;

import com.connecto.DTO.responseDTO.AudioCallResponseDTO;
import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.DTO.responseDTO.VideoCallResponseDTO;
import com.connecto.enums.Status;
import com.connecto.enums.Verdict;
import com.connecto.model.AudioCall;
import com.connecto.model.Friend;
import com.connecto.model.User;
import com.connecto.model.VideoCall;
import com.connecto.repositories.AudioCallRepository;
import com.connecto.repositories.FriendRequestRepository;
import com.connecto.repositories.UserRepository;
import com.connecto.repositories.VideoCallRepository;
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
    private VideoCallRepository videoCallRepository;
    private AudioCallRepository audioCallRepository;

    @Autowired
    public UserServiceImplementation(UserRepository userRepository, AudioCallRepository audioCallRepository, VideoCallRepository videoCallRepository, FriendRequestRepository friendRequestRepository) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.videoCallRepository = videoCallRepository;
        this.audioCallRepository = audioCallRepository;
    }

    @Override
    public Map<String, Object> getAllUsers(User user) throws ExecutionException, InterruptedException {
        List<String> requestsSent = user.getFriendRequestsSent();
        List<String> requestsReceived = user.getFriendRequestsReceived();
        List<Map<String, Object>> response = new ArrayList<>();
        List<QueryDocumentSnapshot> usersSnapshot = userRepository.getAllUsers();
        usersSnapshot.forEach(doc -> {
            if (user.getFriends() == null || !user.getFriends().contains(doc.getId()) && !doc.getId().equals(user.getId())) {
                UserResponseDTO userResponseDTO = doc.toObject(UserResponseDTO.class);
                response.add(new HashMap<>() {{
                    put("requestSent", requestsSent.contains(userResponseDTO.getId()));
                    put("requestReceived", requestsReceived.contains(userResponseDTO.getId()));
                    put("user", userResponseDTO);
                }});
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
        if (friends != null) {
            for (String id : friends) {
                response.add(userRepository.findUserById(id).toObject(Friend.class));
            }
            return new HashMap<>() {{
                put("status", true);
                put("data", response);
            }};
        }
        return new HashMap<>() {{
            put("status", false);
            put("data", new ArrayList<>());
        }};
    }

    @Override
    public Map<String, Object> updateUserProfile(String id, Map<String, Object> object) throws ExecutionException, InterruptedException {
        try {
            UserResponseDTO user = userRepository.updateUser(id, object);
            return new HashMap<>() {{
                put("status", true);
                put("message", "User updated successfully");
                put("user", user);
            }};
        } catch (Exception e) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "Something went wrong");
            }};
        }
    }

    @Override
    public Map<String, Object> getCallLogs(String userId) throws ExecutionException, InterruptedException {
        List<Map<String, Object>> response = new ArrayList<>();

        List<QueryDocumentSnapshot> videoCallLogs = videoCallRepository.getVideoCallLogs(userId);
        List<QueryDocumentSnapshot> audioCallLogs = audioCallRepository.getAudioCallLogs(userId);

        List<VideoCallResponseDTO> videoCalls = new ArrayList<>();
        List<AudioCallResponseDTO> audioCalls = new ArrayList<>();

        videoCallLogs.forEach(doc -> {
            VideoCall videoCall = doc.toObject(VideoCall.class);
            //Generate a response
            try {
                UserResponseDTO from = userRepository.findUserById(videoCall.getFrom()).toObject(UserResponseDTO.class);
                UserResponseDTO to = userRepository.findUserById(videoCall.getTo()).toObject(UserResponseDTO.class);

                VideoCallResponseDTO temp = new VideoCallResponseDTO()
                        .setId(videoCall.getId())
                        .setFrom(from)
                        .setTo(to)
                        .setParticipants(videoCall.getParticipants())
                        .setStatus(videoCall.getStatus())
                        .setVerdict(videoCall.getVerdict())
                        .setEndedAt(videoCall.getEndedAt())
                        .setStartedAt(videoCall.getStartedAt());
                videoCalls.add(temp);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        audioCallLogs.forEach(doc -> {
            AudioCall audioCall = doc.toObject(AudioCall.class);
            //Generate a response
            try {
                UserResponseDTO from = userRepository.findUserById(audioCall.getFrom()).toObject(UserResponseDTO.class);
                UserResponseDTO to = userRepository.findUserById(audioCall.getTo()).toObject(UserResponseDTO.class);

                AudioCallResponseDTO temp = new AudioCallResponseDTO()
                        .setId(audioCall.getId())
                        .setFrom(from)
                        .setTo(to)
                        .setParticipants(audioCall.getParticipants())
                        .setStatus(audioCall.getStatus())
                        .setVerdict(audioCall.getVerdict())
                        .setEndedAt(audioCall.getEndedAt())
                        .setStartedAt(audioCall.getStartedAt());
                audioCalls.add(temp);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        for (VideoCallResponseDTO entry : videoCalls) {
            boolean missed = entry.getVerdict() != (Verdict.ACCEPTED);
            if (entry.getFrom().getId().equals(userId)) {
                UserResponseDTO otherUser = entry.getTo();
                response.add(new HashMap<>() {{
                    put("id", entry.getId());
                    put("img", otherUser.getAvatar());
                    put("name", otherUser.getFirstName());
                    put("friend_id", otherUser.getId());
                    put("online", otherUser.getStatus() == Status.ONLINE);
                    put("incoming", false);
                    put("missed", missed);
                    put("type", "video");
                    put("startedAt", entry.getStartedAt());
                    put("endedAt", entry.getEndedAt());
                }});
            } else {
                UserResponseDTO otherUser = entry.getFrom();
                response.add(new HashMap<>() {{
                    put("id", entry.getId());
                    put("img", otherUser.getAvatar());
                    put("name", otherUser.getFirstName());
                    put("friend_id", otherUser.getId());
                    put("online", otherUser.getStatus() == Status.ONLINE);
                    put("incoming", true);
                    put("missed", missed);
                    put("type", "video");
                    put("startedAt", entry.getStartedAt());
                    put("endedAt", entry.getEndedAt());
                }});
            }
        }

        for (AudioCallResponseDTO entry : audioCalls) {
            boolean missed = entry.getVerdict() != (Verdict.ACCEPTED);
            if (entry.getFrom().getId().equals(userId)) {
                UserResponseDTO otherUser = entry.getTo();
                response.add(new HashMap<>() {{
                    put("id", entry.getId());
                    put("img", otherUser.getAvatar());
                    put("name", otherUser.getFirstName());
                    put("friend_id", otherUser.getId());
                    put("online", otherUser.getStatus() == Status.ONLINE);
                    put("incoming", false);
                    put("missed", missed);
                    put("type", "audio");
                    put("startedAt", entry.getStartedAt());
                    put("endedAt", entry.getEndedAt());
                }});
            } else {
                UserResponseDTO otherUser = entry.getFrom();
                response.add(new HashMap<>() {{
                    put("id", entry.getId());
                    put("img", otherUser.getAvatar());
                    put("name", otherUser.getFirstName());
                    put("friend_id", otherUser.getId());
                    put("online", otherUser.getStatus() == Status.ONLINE);
                    put("incoming", true);
                    put("missed", missed);
                    put("type", "audio");
                    put("startedAt", entry.getStartedAt());
                    put("endedAt", entry.getEndedAt());
                }});
            }
        }
        return new HashMap<>() {{
            put("status", true);
            put("message", "Call Logs Fetched Successfully");
            put("data", response);
        }};
    }

    @Override
    public Map<String, Object> getUserProfile(String userId) throws ExecutionException, InterruptedException {
        UserResponseDTO user = userRepository.findUserById(userId).toObject(UserResponseDTO.class);
        return new HashMap<>() {{
            put("status", true);
            put("message", "User details fetched successfully");
            put("user", user);
        }};
    }
}
