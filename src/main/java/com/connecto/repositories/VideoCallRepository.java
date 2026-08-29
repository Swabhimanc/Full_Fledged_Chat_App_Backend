package com.connecto.repositories;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;
import com.connecto.model.VideoCall;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class VideoCallRepository {
    private final CollectionReference videoCallRef;

    public VideoCallRepository(Firestore firestore) {
        this.videoCallRef = firestore.collection("VideoCallMaster");
    }

    public WriteResult createCallLog(VideoCall videoCall) throws ExecutionException, InterruptedException {
        DocumentReference newVideoCallRef = videoCallRef.document();
        videoCall.setId(newVideoCallRef.getId());
        return newVideoCallRef.set(videoCall).get();
    }

    public void updateVideoCallState(String to, String from, Verdict verdict, Status status) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> doc1 = videoCallRef
                .whereEqualTo("participants",List.of(from,to))
                .get()
                .get()
                .getDocuments();
        List<QueryDocumentSnapshot> doc2 = videoCallRef
                .whereEqualTo("participants",List.of(to,from))
                .get()
                .get()
                .getDocuments();

        if(!doc1.isEmpty()){
            doc1.get(0).getReference().update(new HashMap<>(){{
                put("verdict",verdict);
                put("status",status);
            }});
        } else if (!doc2.isEmpty()) {
            doc2.get(0).getReference().update(new HashMap<>(){{
                put("verdict",verdict);
                put("status",status);
            }});
        }
    }

    public boolean isParticipant(String callId, String userId) throws ExecutionException, InterruptedException {
        DocumentSnapshot call = videoCallRef.document(callId).get().get();
        List<String> participants = call.exists() ? (List<String>) call.get("participants") : null;
        return participants != null && participants.contains(userId);
    }

    public List<QueryDocumentSnapshot> getVideoCallLogs(String userId) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> response = videoCallRef.whereArrayContains("participants",userId).get().get().getDocuments();
        return response;
    }

    public List<QueryDocumentSnapshot> getVideoCallLogs(String userId, int limit) throws ExecutionException, InterruptedException {
        return videoCallRef
                .whereArrayContains("participants", userId)
                .orderBy("startedAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .get()
                .getDocuments();
    }
}
