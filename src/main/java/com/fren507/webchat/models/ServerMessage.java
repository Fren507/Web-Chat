package com.fren507.webchat.models;

import java.util.List;
import java.util.UUID;

public class ServerMessage {
    private String messageType;
    private String message;
    private List<UUID> affectedPlayers;

    // Default constructor is required for Jackson JSON parsing!
    public ServerMessage() {
    }

    public ServerMessage(String messageType, String message, List<UUID> affectedPlayers) {
        this.messageType = messageType;
        this.message = message;
        this.affectedPlayers = affectedPlayers;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<UUID> getAffectedPlayers() {
        return affectedPlayers;
    }

    public void setAffectedPlayers(List<UUID> affectedPlayers) {
        this.affectedPlayers = affectedPlayers;
    }
}
