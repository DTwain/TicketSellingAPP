package org.example.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.domain.UserType;
import org.example.security.JwtUtil;
import org.example.service.ServicesException;
import org.example.service.interfaces.MatchServiceInterface;
import org.example.service.interfaces.UserServiceInterface;
import org.example.websocket.MatchWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/matches")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class MatchRestController {

    @Autowired
    private MatchServiceInterface matchService;

    @Autowired
    private UserServiceInterface userService;

    @Autowired
    private JwtUtil jwtUtil;

    private static final Logger logger = LogManager.getLogger(MatchRestController.class);

    /**
     * Get all matches - requires authentication
     */
    @GetMapping
    public ResponseEntity<?> getAllMatches(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Check authentication
            if (!isAuthenticated(authHeader)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            logger.info("REST: Getting all matches");
            List<Match> matches = matchService.findAll();

            // Log WebSocket connection count
            int wsConnections = MatchWebSocketHandler.getConnectedClientsCount();
            logger.info("Retrieved {} matches. WebSocket clients connected: {}", matches.size(), wsConnections);

            return ResponseEntity.ok(matches);
        } catch (ServicesException e) {
            logger.error("Error getting all matches: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve matches"));
        }
    }

    /**
     * Get match by ID - requires authentication
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMatchById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Check authentication
            if (!isAuthenticated(authHeader)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            logger.info("REST: Getting match by id: {}", id);
            Optional<Match> match = matchService.findOne(id);
            if (match.isPresent()) {
                return ResponseEntity.ok(match.get());
            } else {
                logger.warn("Match with id {} not found", id);
                return ResponseEntity.notFound().build();
            }
        } catch (ServicesException e) {
            logger.error("Error getting match by id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve match"));
        }
    }

    /**
     * Create new match - requires seller authentication
     */
    @PostMapping
    public ResponseEntity<?> createMatch(
            @RequestBody Match match,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Check authentication and seller role
            if (!isAuthenticatedSeller(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Seller authentication required"));
            }

            logger.info("REST: Creating match: {} vs {}", match.getTeamA(), match.getTeamB());

            // Ensure ID is null for new entities
            match.setId(null);

            Match savedMatch = matchService.save(match);

            // Broadcast WebSocket notification for match creation
            try {
                logger.info("Broadcasting WebSocket notification for new match: {}", savedMatch.getId());
                MatchWebSocketHandler.broadcastMatchUpdate(savedMatch, "CREATED");
            } catch (Exception wsError) {
                logger.error("Error broadcasting WebSocket notification for match creation: {}", wsError.getMessage());
                // Don't fail the REST operation due to WebSocket errors
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(savedMatch);
        } catch (ServicesException e) {
            logger.error("Error creating match: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create match: " + e.getMessage()));
        }
    }

    /**
     * Update existing match - requires seller authentication
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMatch(
            @PathVariable Long id,
            @RequestBody Match match,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Check authentication and seller role
            if (!isAuthenticatedSeller(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Seller authentication required"));
            }

            logger.info("REST: Updating match with id: {}", id);

            // Check if match exists first
            Optional<Match> existingMatch = matchService.findOne(id);
            if (existingMatch.isEmpty()) {
                logger.warn("Cannot update - match with id {} not found", id);
                return ResponseEntity.notFound().build();
            }

            // Set the ID from path parameter
            match.setId(id);

            Match updatedMatch = matchService.update(match);

            // Broadcast WebSocket notification for match update
            try {
                logger.info("Broadcasting WebSocket notification for updated match: {}", updatedMatch.getId());
                MatchWebSocketHandler.broadcastMatchUpdate(updatedMatch, "UPDATED");
            } catch (Exception wsError) {
                logger.error("Error broadcasting WebSocket notification for match update: {}", wsError.getMessage());
                // Don't fail the REST operation due to WebSocket errors
            }

            logger.info("REST: Successfully updated match with id: {}", id);
            return ResponseEntity.ok(updatedMatch);
        } catch (ServicesException e) {
            logger.error("Error updating match with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to update match: " + e.getMessage()));
        }
    }

    /**
     * Delete match - requires seller authentication
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMatch(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Check authentication and seller role
            if (!isAuthenticatedSeller(authHeader)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Seller authentication required"));
            }

            logger.info("REST: Deleting match with id: {}", id);

            // Check if match exists first and get the match data for WebSocket notification
            Optional<Match> existingMatch = matchService.findOne(id);
            if (existingMatch.isEmpty()) {
                logger.warn("Cannot delete - match with id {} not found", id);
                return ResponseEntity.notFound().build();
            }

            // Store match data before deletion for WebSocket notification
            Match matchToDelete = existingMatch.get();

            matchService.delete(id);

            // Broadcast WebSocket notification for match deletion
            try {
                logger.info("Broadcasting WebSocket notification for deleted match: {}", id);
                MatchWebSocketHandler.broadcastMatchUpdate(matchToDelete, "DELETED");
            } catch (Exception wsError) {
                logger.error("Error broadcasting WebSocket notification for match deletion: {}", wsError.getMessage());
                // Don't fail the REST operation due to WebSocket errors
            }

            logger.info("REST: Successfully deleted match with id: {}", id);
            return ResponseEntity.noContent().build();
        } catch (ServicesException e) {
            logger.error("Error deleting match with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete match: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint to verify WebSocket connectivity
     */
    @GetMapping("/websocket/status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        int connectedClients = MatchWebSocketHandler.getConnectedClientsCount();

        return ResponseEntity.ok(Map.of(
                "connectedClients", connectedClients,
                "status", connectedClients > 0 ? "active" : "no_connections",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Test endpoint to send a test WebSocket message
     */
    @PostMapping("/websocket/test")
    public ResponseEntity<Map<String, Object>> sendTestMessage(
            @RequestBody(required = false) String message,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Check authentication for test endpoint
            if (!isAuthenticated(authHeader)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            String testMessage = message != null ? message : "Test message from REST API";
            MatchWebSocketHandler.broadcastMessage("TEST_MESSAGE", testMessage);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test message sent to " + MatchWebSocketHandler.getConnectedClientsCount() + " clients",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("Error sending test WebSocket message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }

    // Helper methods for authentication
    private boolean isAuthenticated(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authHeader.substring(7);
        return jwtUtil.validateToken(token);
    }

    private boolean isAuthenticatedSeller(String authHeader) {
        if (!isAuthenticated(authHeader)) {
            return false;
        }

        try {
            String token = authHeader.substring(7);
            UserType userType = jwtUtil.getUserTypeFromToken(token);
            return userType == UserType.SELLER;
        } catch (Exception e) {
            logger.error("Error checking seller authentication: {}", e.getMessage());
            return false;
        }
    }
}