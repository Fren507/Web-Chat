package com.fren507.webchat.website_backend;

import com.fren507.mcauth.api.TokenAPI;
import com.fren507.mcauth.model.TokenData;
import com.fren507.webchat.managers.VerifiedProfileManager;
import com.fren507.webchat.models.VerifiedProfile;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebServer {

    private static final Gson gson = new Gson();
    private static final long CACHE_DURATION_MS = 30 * 60 * 1000; // 30 Minuten
    private final int port;
    private final VerifiedProfileManager manager;
    private final Logger LOGGER;
    // Speichert UUID als String → CachedSkin (Gültig z.B. für 30 Minuten)
    private final Map<String, CachedSkin> skinCache = new ConcurrentHashMap<>();
    private HttpServer server;
    private boolean isRunning;

    public WebServer(int port, VerifiedProfileManager manager, Logger LOGGER) {
        this.port = port;
        this.manager = manager;
        this.LOGGER = LOGGER;
    }

    private static String fetchProfile(UUID uuid) throws Exception {
        String cleanUuid = uuid.toString().replace("-", "");

        URL url = new URL(
                "https://sessionserver.mojang.com/session/minecraft/profile/" + cleanUuid
        );

        try (var input = url.openStream()) {
            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    public void start() throws Exception {
        server = HttpServer.create(
                new InetSocketAddress(port),
                0
        );
        isRunning = true;

        server.createContext("/verify", exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (!exchange.getRequestMethod().equals("POST")) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String token = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            boolean valid = !token.isEmpty()
                    && token.matches("^[A-Z0-9]{12}$")
                    && TokenAPI.isTokenValid(token);

            if (!valid) {
                sendJson(exchange, 401, new VerifyResponse(false, null));
                return;
            }

            Optional<TokenData> tokenData = TokenAPI.getToken(token);

            if (tokenData.isEmpty()) {
                sendJson(exchange, 401, new VerifyResponse(false, null));
                return;
            }

            VerifiedProfile profile = manager.createVerifiedProfile(
                    tokenData.get()
            );

            sendJson(exchange, 200, new VerifyResponse(true, profile));
        });

        // --- HIER IST DER CACHE INTEGRIERT ---
        server.createContext("/skins", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            if (parts.length < 3) {
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
                return;
            }

            String playerUUID = parts[2];

            try {
                UUID uuid;

                if (playerUUID.length() == 32) {
                    playerUUID = playerUUID.replaceFirst(
                            "(.{8})(.{4})(.{4})(.{4})(.{12})",
                            "$1-$2-$3-$4-$5"
                    );
                }

                uuid = UUID.fromString(playerUUID);
                String cacheKey = uuid.toString();
                String response = null;

                // 1. Prüfen, ob ein gültiger Eintrag im Cache liegt
                CachedSkin cached = skinCache.get(cacheKey);
                if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_DURATION_MS) {
                    response = cached.profileJson;
                } else {
                    // 2. Wenn nicht im Cache oder abgelaufen -> Neu von Mojang fetchen
                    try {
                        response = fetchProfile(uuid);
                        // Im Cache speichern
                        skinCache.put(cacheKey, new CachedSkin(response));
                    } catch (Exception e) {
                        LOGGER.error("Failed to fetch profile for {}", uuid, e);

                        // Falls Mojang blockiert, aber wir zufällig noch alte Daten haben, nutzen wir die Notfall-Daten
                        if (cached != null) {
                            response = cached.profileJson;
                            LOGGER.warn("Using expired cache for {} due to fetch error.", uuid);
                        } else {
                            exchange.sendResponseHeaders(500, -1);
                            exchange.close();
                            return;
                        }
                    }
                }

                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                        "Content-Type",
                        "application/json"
                );

                exchange.getResponseHeaders().set(
                        "Access-Control-Allow-Origin",
                        "*"
                );

                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(bytes);
                }

            } catch (IllegalArgumentException e) {
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
            }
        });

        server.start();
    }

    public void stop() {
        server.stop(0);
        isRunning = false;
    }

    private void sendJson(
            HttpExchange exchange,
            int status,
            VerifyResponse response
    ) {
        try {
            String json = gson.toJson(response);

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .add("Content-Type", "application/json; charset=UTF-8");

            exchange.sendResponseHeaders(status, bytes.length);

            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isStopped() {
        return !isRunning;
    }

    // --- CACHE STRUKTUR ---
    private static class CachedSkin {
        final String profileJson;
        final long timestamp;

        CachedSkin(String profileJson) {
            this.profileJson = profileJson;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private record VerifyResponse(
            boolean valid,
            VerifiedProfile profile
    ) {
    }
}
