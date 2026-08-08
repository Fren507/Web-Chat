package com.fren507.webchat;

import com.fren507.webchat.managers.VerifiedProfileManager;
import com.fren507.webchat.website_backend.WebChatServer;
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
        LOGGER.info("Starting Web-Chat!");

        VerifiedProfileManager manager = new VerifiedProfileManager();
        WebChatServer server = new WebChatServer(9123, manager, LOGGER, URI.create("https://github.com/Fren507/Web-Chat/"));

        ServerLifecycleEvents.SERVER_STARTED.register(minecraftServer -> {
            try {
                server.start(minecraftServer);
            } catch (Exception e) {
                LOGGER.error("Failed to start WebServer", e);
            }
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            server.sendMessageToWeb(message.signedContent(), sender.getPlainTextName(), sender.getUUID());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, minecraftServer) -> {
            String playerName = handler.getPlayer().getPlainTextName();
            UUID playerUUID = handler.getPlayer().getUUID();

            // Broadcast a custom system message or join event to the web
            server.sendServerMessageToWeb("PlayerJoin", playerName, List.of(playerUUID));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, minecraftServer) -> {
            String playerName = handler.getPlayer().getPlainTextName();
            UUID playerUUID = handler.getPlayer().getUUID();

            // Broadcast leave event to the web
            server.sendServerMessageToWeb("PlayerLeave", playerName, List.of(playerUUID));
        });

        ServerMessageEvents.GAME_MESSAGE.register((minecraftServer, message, overlay) -> {
            if (!overlay) { // Skip action bar messages
                server.sendServerMessageToWeb("GameMessage", message.getString(), List.of());
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(minecraftServer -> {
            server.stop();
        });
    }

}
