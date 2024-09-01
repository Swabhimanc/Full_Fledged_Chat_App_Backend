package com.connecto.repositories;


import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;

@Repository
public class OTPRepository{
    private final CollectionReference otpRef;

    public OTPRepository(Firestore firestore){
        this.otpRef = firestore.collection("OTPMaster");
    }
    public CollectionReference getOtpRef(){
        return this.otpRef;
    }

    public ApiFuture<WriteResult> saveOtp(String email, String otp, Date validTill) {
        DocumentReference otpDoc = otpRef.document();
        return otpDoc.create(new HashMap<String, Object>() {{
            put("createdBy", email);
            put("otp", otp);
            put("createdAt",new Date());
            put("validTill", validTill);
        }});
    }
}

