package com.fren507.webchat;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

public class WebSocketServer extends org.java_websocket.server.WebSocketServer {

    public WebSocketServer(int port) {
        super(new java.net.InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Client verbunden: " + conn.getRemoteSocketAddress());

        // Greeting only to the newly connected user
        conn.send("Verbunden mit Web Chat!");

        // Notify everyone else that someone joined
        broadcast("Ein neuer Client ist dem Chat beigetreten!");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Client getrennt: " + reason);

        // Notify all remaining users
        broadcast("Ein Client hat den Chat verlassen.");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Nachricht received: " + message);

        // Instead of echo (conn.send), send to ALL connected clients!
        broadcast(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket Server gestartet!");
    }
}