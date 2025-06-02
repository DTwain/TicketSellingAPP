package org.example.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.User;
import org.example.domain.UserType;
import org.example.security.JwtUtil;
import org.example.service.ServicesException;
import org.example.service.interfaces.UserServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class AuthController {

    @Autowired
    private UserServiceInterface userService;

    @Autowired
    private JwtUtil jwtUtil;

    private static final Logger logger = LogManager.getLogger(AuthController.class);

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login attempt for username: {}", loginRequest.getUsername());

            if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Username and password are required"));
            }

            // Authenticate user
            UserType userType = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

            if (userType == UserType.UNKNOWN) {
                logger.warn("Failed login attempt for username: {}", loginRequest.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid username or password"));
            }

            // Get user details
            Optional<User> userOpt = userService.getUserByUsername(loginRequest.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("User not found after authentication"));
            }

            User user = userOpt.get();

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), userType);

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("user", createUserResponse(user, userType));

            logger.info("Successful login for user: {} ({})", user.getUsername(), userType);
            return ResponseEntity.ok(response);

        } catch (ServicesException e) {
            logger.error("Service error during login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Authentication service error"));
        } catch (Exception e) {
            logger.error("Unexpected error during login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    /**
     * Signup endpoint
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody SignupRequest signupRequest) {
        try {
            logger.info("Signup attempt for username: {}", signupRequest.getUsername());

            // Validate input
            if (signupRequest.getUsername() == null || signupRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Username is required"));
            }

            if (signupRequest.getPassword() == null || signupRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Password is required"));
            }

            if (signupRequest.getPassword().length() < 3) {
                return ResponseEntity.badRequest().body(createErrorResponse("Password must be at least 3 characters long"));
            }

            // Check if username already exists
            if (userService.usernameExists(signupRequest.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Username already exists"));
            }

            // Register user
            boolean registered = userService.registerUser(signupRequest.getUsername(), signupRequest.getPassword());

            if (!registered) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Failed to register user"));
            }

            // Get the newly created user
            Optional<User> userOpt = userService.getUserByUsername(signupRequest.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("User not found after registration"));
            }

            User user = userOpt.get();
            UserType userType = UserType.USER; // New users are regular clients by default

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), userType);

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account created successfully");
            response.put("token", token);
            response.put("user", createUserResponse(user, userType));

            logger.info("Successful signup for user: {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ServicesException e) {
            logger.error("Service error during signup: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Registration service error"));
        } catch (Exception e) {
            logger.error("Unexpected error during signup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    /**
     * Validate token endpoint
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody TokenValidationRequest request) {
        try {
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Token is required"));
            }

            boolean isValid = jwtUtil.validateToken(request.getToken());

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or expired token"));
            }

            // Extract user information from token
            String username = jwtUtil.getUsernameFromToken(request.getToken());
            Long userId = jwtUtil.getUserIdFromToken(request.getToken());
            UserType userType = jwtUtil.getUserTypeFromToken(request.getToken());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", true);
            response.put("user", Map.of(
                    "id", userId,
                    "username", username,
                    "userType", userType.toString(),
                    "role", userType == UserType.SELLER ? "SELLER" : "CLIENT"
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Token validation error"));
        }
    }

    /**
     * Get current user info from token
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authorization header required"));
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or expired token"));
            }

            String username = jwtUtil.getUsernameFromToken(token);
            Optional<User> userOpt = userService.getUserByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("User not found"));
            }

            User user = userOpt.get();
            UserType userType = jwtUtil.getUserTypeFromToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", createUserResponse(user, userType));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving user information"));
        }
    }

    /**
     * Logout endpoint (for client-side token invalidation)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    // Helper methods
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

    private Map<String, Object> createUserResponse(User user, UserType userType) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("userType", userType.toString());
        userResponse.put("role", userType == UserType.SELLER ? "SELLER" : "CLIENT");
        userResponse.put("isSeller", userType == UserType.SELLER);
        userResponse.put("isClient", userType == UserType.USER);
        return userResponse;
    }

    // Request DTOs
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class SignupRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class TokenValidationRequest {
        private String token;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}