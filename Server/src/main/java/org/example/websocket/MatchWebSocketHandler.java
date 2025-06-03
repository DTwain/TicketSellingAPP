package org.example.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class MatchWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LogManager.getLogger(MatchWebSocketHandler.class);
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        logger.info("WebSocket connection established from {}. Session ID: {}, Total connections: {}",
                session.getRemoteAddress(), session.getId(), sessions.size());

        // Send welcome message
        Map<String, Object> welcomeMessage = Map.of(
                "type", "CONNECTION_ESTABLISHED",
                "message", "Connected to Basketball Ticket Shop WebSocket",
                "sessionId", session.getId(),
                "totalConnections", sessions.size()
        );

        try {
            String jsonMessage = objectMapper.writeValueAsString(welcomeMessage);
            session.sendMessage(new TextMessage(jsonMessage));
            logger.info("Welcome message sent to session {}", session.getId());
        } catch (Exception e) {
            logger.error("Error sending welcome message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("WebSocket connection closed. Session ID: {}, Status: {}, Total connections: {}",
                session.getId(), status, sessions.size());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.debug("Received WebSocket message from session {}: {}", session.getId(), message.getPayload());

        // Handle ping/pong or other client messages if needed
        try {
            Map<String, Object> clientMessage = objectMapper.readValue(message.getPayload(), Map.class);
            String messageType = (String) clientMessage.get("type");

            if ("PING".equals(messageType)) {
                Map<String, Object> pongResponse = Map.of(
                        "type", "PONG",
                        "timestamp", System.currentTimeMillis()
                );
                String jsonResponse = objectMapper.writeValueAsString(pongResponse);
                session.sendMessage(new TextMessage(jsonResponse));
            }
        } catch (Exception e) {
            logger.warn("Error handling client message: {}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    /**
     * Broadcast match update to all connected clients
     */
    public static void broadcastMatchUpdate(Match match, String operation) {
        if (sessions.isEmpty()) {
            return;
        }

        logger.info("Broadcasting match {} to {} connected clients", operation, sessions.size());

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules(); // Register JavaTime module for LocalDateTime

            Map<String, Object> message = Map.of(
                    "type", "MATCH_UPDATED",
                    "operation", operation,
                    "match", match,
                    "timestamp", System.currentTimeMillis()
            );

            String jsonMessage = mapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            // Send to all connected sessions
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(textMessage);
                        }
                        logger.debug("Sent update to session: {}", session.getId());
                    } catch (IOException e) {
                        logger.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                        sessions.remove(session);
                    }
                } else {
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            logger.error("Error creating WebSocket message: {}", e.getMessage(), e);
        }
    }

    /**
     * Get current number of connected clients
     */
    public static int getConnectedClientsCount() {
        return sessions.size();
    }

    /**
     * Send a custom message to all connected clients
     */
    public static void broadcastMessage(String type, Object data) {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> message = Map.of(
                    "type", type,
                    "data", data,
                    "timestamp", System.currentTimeMillis()
            );

            String jsonMessage = mapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(textMessage);
                        }
                    } catch (IOException e) {
                        logger.error("Error sending custom message to session {}: {}", session.getId(), e.getMessage());
                        sessions.remove(session);
                    }
                } else {
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            logger.error("Error broadcasting custom message: {}", e.getMessage(), e);
        }
    }
}