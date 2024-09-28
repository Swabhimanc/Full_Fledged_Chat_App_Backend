package com.connecto.model;


import com.connecto.enums.Status;
import com.google.cloud.firestore.annotation.IgnoreExtraProperties;

import java.util.*;
import java.util.regex.Pattern;

@IgnoreExtraProperties
public class User {
    private String id;
    private String firstName;
    private String lastName;
    private String avatar;
    private String email;
    private String password;
    private Date passwordChangedAt;
    private String passwordResetToken;
    private Date passwordResetExpires;
    private Date createdAt = new Date();
    private Date updatedAt = new Date();
    private boolean verified = false;
    private String otp;
    private Date otpExpiry;
    private Status status;
    private List<String> friends = new ArrayList<>();
    private List<String> friendRequestsSent = new ArrayList<>();
    private List<String> friendRequestsReceived = new ArrayList<>();
    private String about;

    public User() {

    }

    public User(Map reqObj) {
        this.firstName = (String) reqObj.get("firstName");
        this.lastName = (String) reqObj.get("lastName");
        this.email = (String) reqObj.get("email");
        this.password = (String) reqObj.get("password");
    }

    public static List<String> validate(User user) {
        List<String> errors = new ArrayList<>();

        if (user.getFirstName() == null || user.getFirstName().length() < 3 || user.getFirstName().length() > 20) {
            errors.add("Username must be a string between 3 and 20 characters");
        }

        if (user.getEmail() == null || !isValidEmail(user.getEmail())) {
            errors.add("Invalid email format");
        }

        if (user.getPassword() == null || user.getPassword().length() < 8) {
            errors.add("Password must be at least 8 characters long");
        }

        return errors;
    }

    private static boolean isValidEmail(String email) {
        String emailRegex = "^[\\w-\\.]+@[\\w-]+\\.[\\w-]{2,4}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    public List<String> getFriendRequestsReceived() {
        return friendRequestsReceived;
    }

    public void setFriendRequestsReceived(List<String> friendRequestsReceived) {
        this.friendRequestsReceived = friendRequestsReceived;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public List<String> getFriendRequestsSent() {
        return friendRequestsSent;
    }

    public void setFriendRequestsSent(List<String> friendRequestsSent) {
        this.friendRequestsSent = friendRequestsSent;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Map<?, ?> toMap() {
        return new HashMap<>() {{
            put("firstName", getFirstName());
            put("lastName", getLastName());
            put("email", getEmail());
            put("id", getId());
        }};
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(Date passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public Date getPasswordResetExpires() {
        return passwordResetExpires;
    }

    public void setPasswordResetExpires(Date passwordResetExpires) {
        this.passwordResetExpires = passwordResetExpires;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getOtpExpiry() {
        return otpExpiry;
    }

    public void setOtpExpiry(Date otpExpiry) {
        this.otpExpiry = otpExpiry;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public void updateFields(Map<String, Object> reqObj) {
        this.firstName = (String) reqObj.get("firstName");
        this.lastName = (String) reqObj.get("lastName");
        this.email = (String) reqObj.get("email");
        this.password = (String) reqObj.get("password");
    }
}
