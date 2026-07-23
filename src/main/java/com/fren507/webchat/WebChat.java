package com.fren507.webchat;

import com.fren507.webchat.managers.VerifiedProfileManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebChat implements ModInitializer {
    public static final String MOD_ID = "web-chat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");

        WebSocketServer socket = new WebSocketServer(9092);
        VerificationAPI api = new VerificationAPI();
        VerifiedProfileManager manager = new VerifiedProfileManager();

        try {
            socket.start();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        try {
            api.start(8080, manager);
        } catch (Exception e) {
            LOGGER.error("Failed to start API", e);
        }
    }
}
