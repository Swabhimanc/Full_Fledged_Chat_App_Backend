package com.connecto.repositories;

import com.connecto.enums.Status;
import com.connecto.enums.Verdict;
import com.connecto.model.AudioCall;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class AudioCallRepository {
    private final CollectionReference audioCallRef;

    public AudioCallRepository(Firestore firestore) {
        this.audioCallRef = firestore.collection("AudioCallMaster");
    }

    public WriteResult createCallLog(AudioCall audioCall) throws ExecutionException, InterruptedException {
        DocumentReference newAudioCallRef = audioCallRef.document();
        audioCall.setId(newAudioCallRef.getId());
        return newAudioCallRef.set(audioCall).get();
    }

    public void updateAudioCallState(String to, String from, Verdict verdict, Status status) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> doc1 = audioCallRef
                .whereEqualTo("participants",List.of(from,to))
                .get()
                .get()
                .getDocuments();
        List<QueryDocumentSnapshot> doc2 = audioCallRef
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
    public List<QueryDocumentSnapshot> getAudioCallLogs(String userId) throws ExecutionException, InterruptedException {
        List<QueryDocumentSnapshot> response = audioCallRef.whereArrayContains("participants",userId).get().get().getDocuments();
        return response;
    }
}
