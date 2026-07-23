package com.fren507.webchat;

import com.fren507.mcauth.api.TokenAPI;
import com.fren507.mcauth.model.TokenData;
import com.fren507.webchat.managers.VerifiedProfileManager;
import com.fren507.webchat.models.VerifiedProfile;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class VerificationAPI {

    private static final Gson gson = new Gson();

    public void start(int port, VerifiedProfileManager manager) throws Exception {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(port),
                0
        );

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
                    && token.matches("^[A-Z0-9]{4}(?:-[A-Z0-9]{4}){2}$")
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

        server.start();
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

    private record VerifyResponse(
            boolean valid,
            VerifiedProfile profile
    ) {
    }
}