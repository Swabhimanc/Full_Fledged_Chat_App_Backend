package com.connecto.repositories;

import com.connecto.DTO.responseDTO.UserResponseDTO;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.connecto.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Repository
public class UserRepository {
    /*Document Reference: You can use a DocumentReference to perform CRUD operations on a document.
                          set() or update() to create or modify the document.
                          delete() to remove the document. You can call the get() method on a
                          DocumentReference to retrieve a DocumentSnapshot, which contains the
                          actual data of the document.

      DocumentSnapshot: A DocumentSnapshot is an object that represents the data of a document
                        at a particular point in time. It contains the document’s data and
                        metadata, such as whether the document exists and the time it was last updated.

                        use data() or get(fieldPath) methods to retrieve the data contained in the document
                        exists() to check if the document actually exists in the database*/

    private final CollectionReference usersRef;
    private final CollectionReference otpRef;

    public UserRepository(Firestore firestore) {
        this.usersRef = firestore.collection("UsersMaster");
        this.otpRef = firestore.collection("OTPMaster");
    }

    public CollectionReference getUsersRef(){
        return this.usersRef;
    }

    public User getUserById(String userId) throws ExecutionException, InterruptedException {
        return usersRef.document(userId).get().get().toObject(User.class);
    }
    public QuerySnapshot findOtpByEmail(String email) throws ExecutionException, InterruptedException {
        return otpRef.whereEqualTo("createdBy", email).get().get();
    }

    public QuerySnapshot findUserByUsername(String username) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot =  usersRef.whereEqualTo("", username).get().get();
        querySnapshot.getDocuments().get(0).toObject(User.class);
        return querySnapshot;
    }

    public QuerySnapshot findUserByEmail(String email) throws ExecutionException, InterruptedException {
        return usersRef.whereEqualTo("email", email).get().get();
    }

    public void saveUser(User user) throws ExecutionException, InterruptedException {
        DocumentReference userRef = usersRef.document();
        user.setId(userRef.getId());
        userRef.set(user).get();
    }

    public void updateUser(String userId, String field, Object value) throws ExecutionException, InterruptedException {
        usersRef.document(userId).update(field, value).get();
    }
    public UserResponseDTO updateUser(String userId, Map<String,Object>data) throws ExecutionException, InterruptedException {
        usersRef.document(userId).update(data).get();
        return usersRef.document(userId).get().get().toObject(UserResponseDTO.class);
    }

    public void deleteOtp(String otpId) throws ExecutionException, InterruptedException {
        otpRef.document(otpId).delete().get();
    }

    public List<QueryDocumentSnapshot> getAllUsers() throws ExecutionException, InterruptedException {
        return usersRef.whereEqualTo("verified",true).get().get().getDocuments();
    }

    public DocumentSnapshot findUserById(String userId) throws ExecutionException, InterruptedException {
        return usersRef.document(userId).get().get();
    }
    public DocumentReference findUserReferenceById(String userId){
        return usersRef.document(userId);
    }

    public List<DocumentSnapshot> findUsersByIds(List<String> userIds) throws ExecutionException, InterruptedException {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> uniqueIds = new LinkedHashSet<>(userIds);
        List<ApiFuture<DocumentSnapshot>> futures = new ArrayList<>();

        for (String id : uniqueIds) {
            futures.add(usersRef.document(id).get());
        }

        return ApiFutures.allAsList(futures).get();
    }
}
