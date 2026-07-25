package com.fren507.webchat.models;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jspecify.annotations.Nullable;

import java.net.URI;

public class ChatMessage {
    private String username;
    private String message;

    // Default constructor is required for Jackson JSON parsing!
    public ChatMessage() {
    }

    public ChatMessage(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Component getMessageComponent() {
        return Component.literal(message);
    }

    public Component getUsernamePrefix(@Nullable URI websiteURL) {
        URI url = websiteURL != null
                ? websiteURL
                : URI.create("https://github.com/Fren507/Web-Chat/");

        String hoverText = websiteURL != null
                ? "Visit Web Chat!"
                : "For further information visit the mod on GitHub!";

        return Component
                .literal("<" + username + " ")
                .append(Component.literal("[WEB]")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(
                                        new HoverEvent.ShowText(
                                                Component.literal(hoverText)
                                        )
                                )
                                .withClickEvent(
                                        new ClickEvent.OpenUrl(url)
                                )
                        ))
                .append(Component.literal(">"));
    }

    public Component getChatComponent(@Nullable URI websiteURL) {
        return getUsernamePrefix(websiteURL).copy()
                .append(Component.literal(" "))
                .append(getMessageComponent());
    }
}
