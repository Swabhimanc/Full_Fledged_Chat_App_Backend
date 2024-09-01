package com.connecto.DTO.requestDTO;

import java.util.HashMap;
import java.util.Map;

public class UpdateUserRequestDTO {
    String firstName;
    String lastName;
    String about;
    String avatar;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Map<?, ?> toMap() {
        return new HashMap<>() {{
            put("firstName", getFirstName());
            put("lastName", getLastName());
            put("about", getAbout());
            put("avatar", getAvatar());
        }};
    }
}
