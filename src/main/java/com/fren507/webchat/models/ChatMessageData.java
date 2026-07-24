package com.fren507.webchat.models;

public class ChatMessageData {
    private String sessionID;
    private String message;

    // Standard-Konstruktor wird für die Deserialisierung benötigt
    public ChatMessageData() {
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}