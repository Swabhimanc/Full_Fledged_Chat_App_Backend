package com.connecto.services.implementation;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.connecto.enums.Status;
import com.connecto.enums.Verdict;
import com.connecto.model.AudioCall;
import com.connecto.model.VideoCall;
import com.connecto.repositories.AudioCallRepository;
import com.connecto.repositories.UserRepository;
import com.connecto.services.AudioCallService;
import com.connecto.services.VideoCallService;
import com.google.cloud.firestore.DocumentReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class AudioCallServiceImplementation implements AudioCallService {
    private UserRepository userRepository;
    private AudioCallRepository audioCallRepository;

    @Autowired
    public AudioCallServiceImplementation(UserRepository userRepository, AudioCallRepository audioCallRepository) {
        this.userRepository = userRepository;
        this.audioCallRepository = audioCallRepository;
    }

    @Override
    public Map<String, Object> startAudioCall(String from, String to) throws ExecutionException, InterruptedException {
        // Fetching user details
        DocumentReference fromUserRef = userRepository.findUserReferenceById(from);
        DocumentReference toUserRef = userRepository.findUserReferenceById(to);
        if (!toUserRef.get().get().exists()) {
            return new HashMap<>() {{
                put("status", false);
                put("message", "User Not Found");
            }};
        }

        // Create a new VideoCall document
        AudioCall audioCallLog = new AudioCall();
        audioCallLog.setParticipants(List.of(from, to));
        audioCallLog.setFrom(from);
        audioCallLog.setTo(to);
        audioCallLog.setStatus(Status.ONGOING);

        // Save the video call in the database (assuming a VideoCallService exists)
        audioCallRepository.createCallLog(audioCallLog);

        Map<String, Object> customResponse = new HashMap<>();
        customResponse.put("from", toUserRef.get().get().toObject(UserResponseDTO.class));
        customResponse.put("roomID", audioCallLog.getId());
        customResponse.put("streamID", to);
        customResponse.put("userID", from);
        customResponse.put("userName", fromUserRef.get().get().get("firstName"));

        // Prepare response data
        return new HashMap<>() {{
            put("status", true);          // Respond with 'to' user details
            put("data", customResponse);
        }};
    }

    @Override
    public Map<String, Object> startAudioCall(String from, String to, String roomID) throws ExecutionException, InterruptedException {
        UserResponseDTO fromUser = userRepository.findUserById(from).toObject(UserResponseDTO.class);
        UserResponseDTO toUser = userRepository.findUserById(to).toObject(UserResponseDTO.class);
        return new HashMap<>() {{
            put("from", fromUser);
            put("roomID", roomID);
            put("streamID", from);
            put("userID", to);
            put("userName", toUser.getFirstName());
        }};
    }

    @Override
    public void updateCallRecord(String to, String from, Verdict verdict, Status status) throws ExecutionException, InterruptedException {
        audioCallRepository.updateAudioCallState(to, from, verdict, status);
    }
}
