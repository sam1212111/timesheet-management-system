package com.tms.common.event;

import java.io.Serializable;

public class UserRegisteredEvent implements Serializable {
    private String userId;
    private String fullName;
    private String email;

    public UserRegisteredEvent() {
    }

    public UserRegisteredEvent(String userId, String fullName, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
