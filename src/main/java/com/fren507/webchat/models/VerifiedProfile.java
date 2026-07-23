package com.fren507.webchat.models;

import com.fren507.mcauth.model.TokenData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class VerifiedProfile {
    private final UUID profileUUID;
    private final UUID playerUUID;
    private final transient Instant expires;
    private final transient Instant created;
    private final long expiresLong;
    private final long createdLong;
    private final String token;
    private final String username;
    private boolean valid = true;

    public VerifiedProfile(TokenData tokenData, String token) {
        this.username = tokenData.getPlayerName();
        this.profileUUID = UUID.randomUUID();
        this.playerUUID = tokenData.getPlayerUUID();

        Instant now = Instant.now();
        this.expires = now.plus(7, ChronoUnit.DAYS);
        this.created = now;

        this.expiresLong = expires.toEpochMilli();
        this.createdLong = created.toEpochMilli();

        this.token = token;
    }

    // Getters

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public UUID getProfileUUID() {
        return profileUUID;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public long getCreatedLong() {
        return createdLong;
    }

    public long getExpiresLong() {
        return expiresLong;
    }

    public Instant getCreated() {
        return created;
    }

    public Instant getExpires() {
        return expires;
    }

    public boolean isValid() {
        return valid;
    }

    // Setters

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
