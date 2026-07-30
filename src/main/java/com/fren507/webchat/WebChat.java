package com.fren507.webchat;

import com.fren507.webchat.managers.VerifiedProfileManager;
import com.fren507.webchat.website_backend.WebServer;
import com.fren507.webchat.website_backend.WebSocketServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public class WebChat implements ModInitializer {
    public static final String MOD_ID = "web-chat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");

        VerifiedProfileManager manager = new VerifiedProfileManager();
        WebServer api = new WebServer(8080, manager, LOGGER);
        WebSocketServer socket = new WebSocketServer(9092, manager, LOGGER, URI.create("http://100.109.207.66:5173/"));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                socket.start(server);
            } catch (Exception e) {
                LOGGER.error("Failed to start WebSocket", e);
            }

            try {
                api.start();
            } catch (Exception e) {
                LOGGER.error("Failed to start API", e);
            }
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            socket.sendMessageToWeb(message.signedContent(), sender.getPlainTextName(), sender.getUUID());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String playerName = handler.getPlayer().getPlainTextName();
            UUID playerUUID = handler.getPlayer().getUUID();

            // Broadcast a custom system message or join event to the web
            socket.sendServerMessageToWeb("PlayerJoin", playerName, List.of(playerUUID));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            String playerName = handler.getPlayer().getPlainTextName();
            UUID playerUUID = handler.getPlayer().getUUID();

            // Broadcast leave event to the web
            socket.sendServerMessageToWeb("PlayerLeave", playerName, List.of(playerUUID));
        });


        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            socket.stop();
            api.stop();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (!socket.isStopped())
                socket.stop();
            if (!api.isStopped())
                api.stop();
        });
    }

}

