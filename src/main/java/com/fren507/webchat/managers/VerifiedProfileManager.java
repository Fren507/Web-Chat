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
import java.util.concurrent.CopyOnWriteArrayList;

public class VerifiedProfileManager {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("webchat");
    private static final Path TOKENS_FILE = CONFIG_DIR.resolve("tokens.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<VerifiedProfile> tokens = new CopyOnWriteArrayList<>();

    public VerifiedProfileManager() {
        loadTokens();
    }

    public void loadTokens() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            if (Files.exists(TOKENS_FILE)) {
                try (Reader reader = Files.newBufferedReader(TOKENS_FILE)) {
                    List<VerifiedProfile> loaded = GSON.fromJson(reader, new TypeToken<List<VerifiedProfile>>() {
                    }.getType());
                    if (loaded != null) {
                        tokens.clear();
                        tokens.addAll(loaded);
                    }
                }
            }
            cleanExpiredTokens();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveTokens() {
        try {
            try (Writer writer = Files.newBufferedWriter(TOKENS_FILE)) {
                GSON.toJson(tokens, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VerifiedProfile createVerifiedProfile(TokenData tokenData) {
        VerifiedProfile newVerifiedProfile = new VerifiedProfile(tokenData, TokenGenerator.generateToken());

        tokens.add(newVerifiedProfile);
        saveTokens();

        return newVerifiedProfile;
    }

    public void cleanExpiredTokens() {
        Instant now = Instant.now();
        boolean changed = false;
        for (VerifiedProfile profile : tokens) {
            if (profile.isValid()
                    && profile.getExpires() != null
                    && profile.getExpires().isBefore(now)) {
                profile.setValid(false);
                changed = true;
            }
        }
        if (changed) saveTokens();
    }
}
