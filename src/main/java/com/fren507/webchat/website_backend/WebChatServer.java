package com.fren507.webchat.website_backend;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOChannelInitializer;
import com.corundumstudio.socketio.SocketIOServer;
import com.fren507.mcauth.api.TokenAPI;
import com.fren507.mcauth.model.TokenData;
import com.fren507.webchat.helpers.ChatUtil;
import com.fren507.webchat.managers.TabListManager;
import com.fren507.webchat.managers.VerifiedProfileManager;
import com.fren507.webchat.models.ChatMessage;
import com.fren507.webchat.models.ChatMessageData;
import com.fren507.webchat.models.ServerMessage;
import com.fren507.webchat.models.VerifiedProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Ein kombinierter Server, der HTTP-Routen (/api/..., Statische Dateien)
 * und Socket.IO (WebSockets) auf demselben TCP-Port über Netty bedient.
 */
public class WebChatServer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long CACHE_DURATION_MS = 30 * 60 * 1000;

    private final int port;
    private final VerifiedProfileManager profileManager;
    private final Logger LOGGER;
    private final URI websiteURI;
    private final Map<String, CachedSkin> skinCache = new ConcurrentHashMap<>();

    private SocketIOServer server;
    private boolean isRunning;

    public WebChatServer(int port, VerifiedProfileManager profileManager, Logger LOGGER, URI websiteURI) {
        this.port = port;
        this.profileManager = profileManager;
        this.LOGGER = LOGGER;
        this.websiteURI = websiteURI;
    }

    private static String fetchProfile(UUID uuid) throws Exception {
        String cleanUuid = uuid.toString().replace("-", "");
        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + cleanUuid);
        try (var input = url.openStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public void start(MinecraftServer minecraftServer) {
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(port);

        config.getSocketConfig().setReuseAddress(true);

        // 1. Erlaube Netty, auch Nicht-Socket.IO Anfragen durchzulassen
        config.setAllowCustomRequests(true);

        TabListManager tabListManager = new TabListManager();
        tabListManager.updateHeader(minecraftServer);

        server = new SocketIOServer(config);

        // 2. Ersetze den internen WrongUrlHandler von Socket.IO durch unseren eigenen HTTP-Handler
        server.setPipelineFactory(new SocketIOChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                super.initChannel(ch);
                ch.pipeline().replace(
                        SocketIOChannelInitializer.WRONG_URL_HANDLER,
                        "custom_http_handler",
                        new CustomHttpHandler()
                );
            }
        });

        isRunning = true;

        // --- Socket.IO Event Listener ---
        server.addConnectListener(client -> {
            LOGGER.info("Client connected: {}", client.getSessionId());
            client.sendEvent("welcome", "Connected to Netty-SocketIO server!");
        });

        server.addDisconnectListener(client -> LOGGER.info("Client disconnected: {}", client.getSessionId()));

        server.addEventListener("chatMessage", ChatMessageData.class, (client, data, ackSender) -> {
            String sessionID = data.getSessionID();
            String message = data.getMessage();

            profileManager.getProfileViaSession(sessionID)
                    .map((profile) -> {
                        ChatMessage chatMessage = new ChatMessage(profile.getUsername(), message, profile.getPlayerUUID(), true);
                        server.getBroadcastOperations().sendEvent("newMessage", chatMessage);
                        ChatUtil.broadcast(minecraftServer, chatMessage.getChatComponent(websiteURI));
                        return true;
                    });
        });

        server.start();
        LOGGER.info("🚀 Kombinierter HTTP- & Socket.IO-Server läuft auf Port {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void sendMessageToWeb(String message, String senderUsername, UUID senderUUID) {
        if (server == null) return;
        ChatMessage chatMessage = new ChatMessage(senderUsername, message, senderUUID, false);
        server.getBroadcastOperations().sendEvent("newMessage", chatMessage);
    }

    public void sendServerMessageToWeb(String messageType, @Nullable String message, List<UUID> affectedPlayers) {
        if (server == null) return;
        ServerMessage serverMessage = new ServerMessage(messageType, message, affectedPlayers);
        server.getBroadcastOperations().sendEvent("serverMessage", serverMessage);
    }

    public void stop() {
        if (server != null && isRunning) {
            LOGGER.info("Stoppe Web-Chat Server...");
            isRunning = false;
            try {
                server.stop();
            } catch (Exception e) {
                LOGGER.error("Fehler beim Stoppen des WebChat-Servers", e);
            }
        }
    }

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

    // --- CUSTOM NETTY HTTP HANDLER (Verarbeitet alle REST- & Frontend-Anfragen) ---
    @ChannelHandler.Sharable
    private class CustomHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
            String uri = req.uri();
            HttpMethod method = req.method();

            // Preflight OPTIONS (CORS)
            if (method.equals(HttpMethod.OPTIONS)) {
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
                addCorsHeaders(response);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                return;
            }

            // Route: /api/verify
            if (uri.startsWith("/api/verify")) {
                handleVerify(ctx, req);
                return;
            }

            // Route: /api/skins/...
            if (uri.startsWith("/api/skins")) {
                handleSkins(ctx, uri);
                return;
            }

            // Statische Dateien & Frontend Fallback (SPA)
            handleStaticFiles(ctx, uri);
        }

        private void handleVerify(ChannelHandlerContext ctx, FullHttpRequest req) {
            if (!req.method().equals(HttpMethod.POST)) {
                sendResponse(ctx, METHOD_NOT_ALLOWED, null, "text/plain");
                return;
            }

            String token = req.content().toString(CharsetUtil.UTF_8);
            boolean valid = !token.isEmpty()
                    && token.matches("^[A-Z0-9]{12}$")
                    && TokenAPI.isTokenValid(token);

            if (!valid) {
                sendJsonResponse(ctx, UNAUTHORIZED, GSON.toJson(new VerifyResponse(false, null)));
                return;
            }

            Optional<TokenData> tokenData = TokenAPI.getToken(token);
            if (tokenData.isEmpty()) {
                sendJsonResponse(ctx, UNAUTHORIZED, GSON.toJson(new VerifyResponse(false, null)));
                return;
            }

            VerifiedProfile profile = profileManager.createVerifiedProfile(tokenData.get());
            sendJsonResponse(ctx, OK, GSON.toJson(new VerifyResponse(true, profile)));
        }

        private void handleSkins(ChannelHandlerContext ctx, String uri) {
            String[] parts = uri.split("/");
            if (parts.length < 3) {
                sendResponse(ctx, BAD_REQUEST, null, "text/plain");
                return;
            }

            String playerUUID = parts[2];
            try {
                if (playerUUID.length() == 32) {
                    playerUUID = playerUUID.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
                }

                UUID uuid = UUID.fromString(playerUUID);
                String cacheKey = uuid.toString();
                String responseBody;

                CachedSkin cached = skinCache.get(cacheKey);
                if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_DURATION_MS) {
                    responseBody = cached.profileJson;
                } else {
                    try {
                        responseBody = fetchProfile(uuid);
                        skinCache.put(cacheKey, new CachedSkin(responseBody));
                    } catch (Exception e) {
                        LOGGER.error("Failed to fetch profile for {}", uuid, e);
                        if (cached != null) {
                            responseBody = cached.profileJson;
                            LOGGER.warn("Using expired cache for {} due to fetch error.", uuid);
                        } else {
                            sendResponse(ctx, INTERNAL_SERVER_ERROR, null, "text/plain");
                            return;
                        }
                    }
                }

                sendJsonResponse(ctx, OK, responseBody);

            } catch (IllegalArgumentException e) {
                sendResponse(ctx, BAD_REQUEST, null, "text/plain");
            }
        }

        private void handleStaticFiles(ChannelHandlerContext ctx, String uri) {
            String path = uri;
            int queryIndex = path.indexOf('?');
            if (queryIndex != -1) {
                path = path.substring(0, queryIndex);
            }

            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            String resourcePath = "/web" + path;
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);

            if (inputStream == null) {
                resourcePath = "/web/index.html";
                inputStream = getClass().getResourceAsStream(resourcePath);
            }

            if (inputStream == null) {
                sendResponse(ctx, NOT_FOUND, null, "text/plain");
                return;
            }

            try {
                byte[] bytes = inputStream.readAllBytes();
                inputStream.close();

                String contentType = "text/html; charset=UTF-8";
                if (path.endsWith(".js")) contentType = "application/javascript";
                else if (path.endsWith(".css")) contentType = "text/css";
                else if (path.endsWith(".svg")) contentType = "image/svg+xml";

                ByteBuf content = Unpooled.wrappedBuffer(bytes);
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
                addCorsHeaders(response);

                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } catch (Exception e) {
                sendResponse(ctx, INTERNAL_SERVER_ERROR, null, "text/plain");
            }
        }

        private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String json) {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            ByteBuf content = Unpooled.wrappedBuffer(bytes);
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
            addCorsHeaders(response);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String text, String contentType) {
            ByteBuf content = text != null ? Unpooled.copiedBuffer(text, CharsetUtil.UTF_8) : Unpooled.EMPTY_BUFFER;
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            addCorsHeaders(response);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private void addCorsHeaders(HttpResponse response) {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
        }
    }
}