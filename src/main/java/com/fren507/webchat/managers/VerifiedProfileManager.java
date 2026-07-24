package com.fren507.webchat.managers;

import com.fren507.mcauth.model.TokenData;
import com.fren507.webchat.helpers.TokenGenerator;
import com.fren507.webchat.models.VerifiedProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class VerifiedProfileManager {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("webchat");
    private static final Path PROFILES_FILE = CONFIG_DIR.resolve("profiles.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<VerifiedProfile> profiles = new CopyOnWriteArrayList<>();

    public VerifiedProfileManager() {
        loadProfiles();
    }

    public void loadProfiles() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            if (Files.exists(PROFILES_FILE)) {
                try (Reader reader = Files.newBufferedReader(PROFILES_FILE)) {
                    List<VerifiedProfile> loaded = GSON.fromJson(reader, new TypeToken<List<VerifiedProfile>>() {
                    }.getType());
                    if (loaded != null) {
                        profiles.clear();
                        profiles.addAll(loaded);
                    }
                }
            }
            cleanExpiredProfiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveProfiles() {
        try {
            try (Writer writer = Files.newBufferedWriter(PROFILES_FILE)) {
                GSON.toJson(profiles, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Optional<VerifiedProfile> getProfileViaSession(String sessionID) {
        this.cleanExpiredProfiles();
        return this.profiles.stream().filter((t) -> t.getSessionID().equals(sessionID)).findFirst();
    }

    public VerifiedProfile createVerifiedProfile(TokenData tokenData) {
        VerifiedProfile newVerifiedProfile = new VerifiedProfile(tokenData, TokenGenerator.generateToken());

        profiles.add(newVerifiedProfile);
        saveProfiles();

        return newVerifiedProfile;
    }

    public void cleanExpiredProfiles() {
        Instant now = Instant.now();
        boolean changed = false;
        for (VerifiedProfile profile : profiles) {
            if (profile.isValid()
                    && profile.getExpires() != null
                    && profile.getExpires().isBefore(now)) {
                profile.setValid(false);
                changed = true;
            }
        }
        if (changed) saveProfiles();
    }
}
