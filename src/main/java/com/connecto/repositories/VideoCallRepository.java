package com.connecto.repositories;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;
import com.connecto.model.VideoCall;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                .whereEqualTo("participants",List.of(from,to))
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
}
