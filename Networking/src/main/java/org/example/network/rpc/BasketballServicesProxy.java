package org.example.network.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.*;
import org.example.network.dto.*;
import org.example.network.protocol.Request;
import org.example.network.protocol.RequestType;
import org.example.network.protocol.Response;
import org.example.network.protocol.ResponseType;
import org.example.network.utils.DiagnosticsManager;
import org.example.service.ServicesException;
import org.example.service.interfaces.MatchServiceInterface;
import org.example.service.interfaces.TicketSellerServiceInterface;
import org.example.service.interfaces.TicketServiceInterface;
import org.example.service.interfaces.UserServiceInterface;
import org.example.utils.observer.TicketObserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class BasketballServicesProxy implements UserServiceInterface, MatchServiceInterface, TicketServiceInterface, TicketSellerServiceInterface {

    private final String host;
    private final int port;
    private Socket connection;
    private BufferedReader input;
    private PrintWriter output;
    private final Gson gson;
    private static final Logger logger = LogManager.getLogger(BasketballServicesProxy.class);

    private final Map<Long, TicketObserver> registeredObservers = new ConcurrentHashMap<>();
    private final Map<Long, TicketObserver> observers = new ConcurrentHashMap<>();
    private static BasketballServicesProxy instance;
    private final List<TicketObserver> directObservers = new ArrayList<>();

    private boolean listenerStarted = false;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Set<Long> registeredObserverIds = ConcurrentHashMap.newKeySet();

    private final Map<Long, Match> matchCache = new ConcurrentHashMap<>();
    private long lastMatchCacheUpdate = 0;
    private static final long CACHE_TTL = 2000;

    public enum ConnectionStatus { CONNECTED, DISCONNECTED, RECONNECTING }
    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    private final List<ConnectionStatusListener> connectionListeners = new CopyOnWriteArrayList<>();

    public BasketballServicesProxy(String host, int port) {
        this.host = host;
        this.port = port;
        this.gson = new Gson();
        instance = this;
    }

    public static BasketballServicesProxy getInstance() {
        return instance;
    }

    public interface ConnectionStatusListener {
        void onConnectionStatusChanged(ConnectionStatus status);
    }

    public void addObserverListener(TicketObserver observer) {
        if (observer != null && !directObservers.contains(observer)) {
            directObservers.add(observer);
            logger.info("Added direct observer, current count: {}", directObservers.size());
        }
    }


    /**
     * Update connection status and notify listeners
     */
    private void updateConnectionStatus(ConnectionStatus newStatus) {
        if (connectionStatus != newStatus) {
            connectionStatus = newStatus;

            for (ConnectionStatusListener listener : connectionListeners) {
                try {
                    listener.onConnectionStatusChanged(newStatus);
                } catch (Exception e) {
                    logger.error("Error notifying connection listener", e);
                }
            }
        }
    }


    /**
     * Improved connection handling with retry mechanism
     */
    private void ensureConnected() throws ServicesException {
        DiagnosticsManager.startTiming("ensureConnected");

        int maxRetries = 3;
        int retryCount = 0;
        int retryDelayMs = 1000;

        while (retryCount < maxRetries) {
            try {
                if (connection == null || connection.isClosed()) {
                    updateConnectionStatus(ConnectionStatus.RECONNECTING);
                    connection = new Socket(host, port);
                    input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    output = new PrintWriter(connection.getOutputStream(), true);
                    updateConnectionStatus(ConnectionStatus.CONNECTED);
                    logger.info("Connected to server at {}:{}", host, port);
                    DiagnosticsManager.endTiming("ensureConnected");
                    return;
                } else {
                    // Connection exists and is open
                    updateConnectionStatus(ConnectionStatus.CONNECTED);
                    DiagnosticsManager.endTiming("ensureConnected");
                    return;
                }
            } catch (IOException e) {
                retryCount++;
                logger.warn("Connection attempt {}/{} failed: {}",
                        retryCount, maxRetries, e.getMessage());

                if (retryCount >= maxRetries) {
                    updateConnectionStatus(ConnectionStatus.DISCONNECTED);
                    logger.error("Failed to connect after {} attempts", maxRetries);
                    DiagnosticsManager.endTiming("ensureConnected");
                    throw new ServicesException("Cannot connect to server", e);
                }

                try {
                    Thread.sleep(retryDelayMs);
                    // Exponential backoff
                    retryDelayMs *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    DiagnosticsManager.endTiming("ensureConnected");
                    throw new ServicesException("Connection interrupted", ie);
                }
            }
        }
    }

    /**
     * Enhanced method to process updates with better error handling and notification strategy
     */
    private void processUpdate(String updateJson) {
        try {
            logger.info("CLIENT RECEIVED UPDATE: {}", updateJson);
            DiagnosticsManager.startTiming("processUpdate");
            Response update = gson.fromJson(updateJson, Response.class);

            // Process in sequential order to prevent race conditions
            CompletableFuture.runAsync(() -> {
                try {
                    switch (update.getType()) {
                        case CONSOLIDATED_UPDATE:
                            processConsolidatedUpdate(update);
                            break;
                        case MATCH_UPDATED:
                            processMatchUpdate(update);
                            break;
                        case TICKET_SOLD:
                            processTicketSoldUpdate(update);
                            break;
                        case USER_TICKETS_CHANGED:
                            processUserTicketsUpdate(update);
                            break;
                        default:
                            logger.warn("Received unknown update type: {}", update.getType());
                    }
                } catch (Exception e) {
                    logger.error("Error processing update: {}", e.getMessage(), e);
                } finally {
                    DiagnosticsManager.endTiming("processUpdate");
                }
            });
        } catch (Exception e) {
            logger.error("Error parsing update: {}", e.getMessage(), e);
            DiagnosticsManager.endTiming("processUpdate");
        }
    }

    /**
     * Notify all observers about connection failure
     */
    private void notifyConnectionFailure() {
        updateConnectionStatus(ConnectionStatus.DISCONNECTED);
    }

    /**
     * Continuously listens for updates from the server
     */
    private void listenForUpdates() {
        // Maximum number of reconnection attempts
        final int MAX_RECONNECT_ATTEMPTS = 10;
        int reconnectAttempts = 0;
        int reconnectDelay = 1000; // Start with 1 second delay

        while (!Thread.currentThread().isInterrupted()) {
            Socket updateSocket = null;
            BufferedReader updateInput = null;
            PrintWriter updateOutput = null;

            try {
                String attemptInfo = reconnectAttempts > 0 ?
                        " (reconnect attempt " + reconnectAttempts + ")" : "";
                logger.info("Starting update listener thread{}", attemptInfo);

                // Create a dedicated socket for updates
                updateSocket = new Socket(host, port);
                updateInput = new BufferedReader(
                        new InputStreamReader(updateSocket.getInputStream()));
                updateOutput = new PrintWriter(updateSocket.getOutputStream(), true);

                // Register this connection as an update listener
                Request listenerRequest = new Request(RequestType.START_UPDATE_LISTENER, null);
                String requestJson = gson.toJson(listenerRequest);
                logger.debug("Sending update listener registration: {}", requestJson);
                updateOutput.println(requestJson);

                // Wait for confirmation
                String response = updateInput.readLine();
                if (response == null) {
                    throw new IOException("No response from server for listener registration");
                }

                logger.info("Update listener registration response: {}", response);
                updateConnectionStatus(ConnectionStatus.CONNECTED);

                // Reset reconnect counter on successful connection
                reconnectAttempts = 0;
                reconnectDelay = 1000;

                // Continuously read updates
                String updateLine;
                logger.info("Starting update listener loop");

                while ((updateLine = updateInput.readLine()) != null) {
                    // Validate JSON before processing
                    try {
                        JsonParser.parseString(updateLine);
                        logger.debug("RECEIVED UPDATE: {}", updateLine);
                        final String update = updateLine;
                        // Process update asynchronously
                        CompletableFuture.runAsync(() -> processUpdate(update));
                    } catch (JsonSyntaxException e) {
                        logger.warn("Received invalid JSON update: {}", updateLine);
                    }
                }

                logger.warn("Update listener loop exited - connection may be closed");

            } catch (Exception e) {
                logger.error("ERROR in update listener: {}", e.getMessage());
                listenerStarted = false;
                updateConnectionStatus(ConnectionStatus.DISCONNECTED);
            } finally {
                // Close resources
                try {
                    if (updateInput != null) updateInput.close();
                    if (updateOutput != null) updateOutput.close();
                    if (updateSocket != null) updateSocket.close();
                } catch (IOException e) {
                    logger.error("Error closing update listener resources", e);
                }

                // Attempt reconnection with exponential backoff
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    reconnectAttempts++;
                    updateConnectionStatus(ConnectionStatus.RECONNECTING);
                    try {
                        logger.info("Waiting {} ms before reconnection attempt {}/{}",
                                reconnectDelay, reconnectAttempts, MAX_RECONNECT_ATTEMPTS);
                        Thread.sleep(reconnectDelay);
                        // Exponential backoff with max of 30 seconds
                        reconnectDelay = Math.min(reconnectDelay * 2, 30000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        logger.warn("Update listener interrupted during reconnection wait");
                        break;
                    }
                } else {
                    logger.error("Maximum reconnection attempts reached ({}). " +
                            "Update listener terminated.", MAX_RECONNECT_ATTEMPTS);

                    // Notify all observers of connection failure
                    notifyConnectionFailure();
                    break;
                }
            }
        }
    }

    private void closeConnection() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (connection != null) connection.close();
            connection = null;
            logger.info("Connection closed");
        } catch (IOException e) {
            logger.error("Error closing connection: {}", e.getMessage());
        }
    }

    private Response sendRequest(Request request) throws ServicesException {
        try {
            ensureConnected();
            String requestJson = gson.toJson(request);
            logger.debug("Sending request: {}", requestJson);
            output.println(requestJson);

            String responseJson = input.readLine();
            logger.debug("Received response: {}", responseJson);
            return gson.fromJson(responseJson, Response.class);
        } catch (IOException e) {
            closeConnection();
            logger.error("Error sending request: {}", e.getMessage());
            throw new ServicesException("Error communicating with server", e);
        }
    }

    private void checkResponse(Response response) throws ServicesException {
        if (response.getType() == ResponseType.ERROR) {
            throw new ServicesException(response.getError());
        }
    }


    @Override
    public boolean usernameExists(String username) throws ServicesException {
        UserDTO userDTO = new UserDTO(null, username, null);
        Request request = new Request(RequestType.LOGIN, userDTO);
        Response response = sendRequest(request);
        return response.getType() == ResponseType.USER;
    }

    @Override
    public boolean registerUser(String username, String password) throws ServicesException {
        UserDTO userDTO = new UserDTO(null, username, password);
        Request request = new Request(RequestType.REGISTER, userDTO);
        Response response = sendRequest(request);
        checkResponse(response);
        return response.getType() == ResponseType.OK;
    }

    @Override
    public Optional<User> getUserByUsername(String username) throws ServicesException {
        UserDTO userDTO = new UserDTO(null, username, null);
        Request request = new Request(RequestType.GET_USER_BY_USERNAME, userDTO);
        Response response = sendRequest(request);
        if (response.getType() == ResponseType.USER) {
            // Extract the user object from the response data
            Map<String, Object> dataMap = (Map<String, Object>) response.getData();
            UserDTO responseDTO = gson.fromJson(gson.toJson(dataMap.get("user")), UserDTO.class);
            return Optional.of(DTOUtils.fromDTO(responseDTO));
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> getUserById(Long id) throws ServicesException {
        UserDTO userDTO = new UserDTO(id, null, null);
        Request request = new Request(RequestType.GET_USER_BY_ID, userDTO);
        Response response = sendRequest(request);
        if (response.getType() == ResponseType.USER) {
            // Extract the user object from the response data
            Map<String, Object> dataMap = (Map<String, Object>) response.getData();
            UserDTO responseDTO = gson.fromJson(gson.toJson(dataMap.get("user")), UserDTO.class);
            return Optional.of(DTOUtils.fromDTO(responseDTO));
        }
        return Optional.empty();
    }

    // MatchServiceInterface implementations
    @Override
    public List<Match> findAll() throws ServicesException {
        Request request = new Request(RequestType.GET_ALL_MATCHES, null);
        Response response = sendRequest(request);
        checkResponse(response);

        Type listType = new TypeToken<List<MatchDTO>>(){}.getType();
        List<MatchDTO> matchDTOs = gson.fromJson(gson.toJson(response.getData()), listType);
        return DTOUtils.fromDTOList(matchDTOs);
    }

    @Override
    public Optional<Match> findOne(Long id) throws ServicesException {
        if (id == null) {
            throw new IllegalArgumentException("Match ID cannot be null");
        }

        // Check cache first
        if (matchCache.containsKey(id) &&
                System.currentTimeMillis() - lastMatchCacheUpdate < CACHE_TTL) {
            logger.debug("Cache hit for match ID: {}", id);
            return Optional.of(matchCache.get(id));
        }

        // Cache miss or expired, fetch from server
        logger.debug("Cache miss for match ID: {}, fetching from server", id);
        Request request = new Request(RequestType.GET_MATCH, id);
        Response response = sendRequest(request);

        if (response.getType() == ResponseType.MATCH) {
            MatchDTO matchDTO = gson.fromJson(gson.toJson(response.getData()), MatchDTO.class);
            Match match = DTOUtils.fromDTO(matchDTO);

            // Update cache with the fetched match
            matchCache.put(id, match);
            lastMatchCacheUpdate = System.currentTimeMillis();
            logger.debug("Updated cache with match ID: {}", id);

            return Optional.of(match);
        }

        return Optional.empty();
    }

    // TicketServiceInterface implementations
    @Override
    public int countAvailableTicketsByMatch(Long matchId) throws ServicesException {
        Request request = new Request(RequestType.GET_AVAILABLE_TICKETS, matchId);
        Response response = sendRequest(request);
        checkResponse(response);
        return gson.fromJson(gson.toJson(response.getData()), Integer.class);
    }

    @Override
    public String ticketPriceRangePerMatch(Long matchId) throws ServicesException {
        Request request = new Request(RequestType.GET_PRICE_RANGE, matchId);
        Response response = sendRequest(request);

        // Special handling for PRICE_RANGE response
        if (response.getType() == ResponseType.PRICE_RANGE) {
            return response.getError(); // Get price from error field
        } else {
            checkResponse(response); // Check for errors
            return gson.fromJson(gson.toJson(response.getData()), String.class);
        }
    }

    @Override
    public Double sellTickets(TicketSale ticketSale) throws ServicesException {
        TicketSaleDTO saleDTO = DTOUtils.toDTO(ticketSale);
        Request request = new Request(RequestType.SELL_TICKETS, saleDTO);
        Response response = sendRequest(request);
        checkResponse(response);
        return gson.fromJson(gson.toJson(response.getData()), Double.class);
    }

    @Override
    public List<Ticket> findTicketsBoughtByUser(User user) throws ServicesException {
        try {
            UserDTO userDTO = DTOUtils.toDTO(user);
            Request request = new Request(RequestType.GET_USER_TICKETS, userDTO);
            Response response = sendRequest(request);

            // Check if this is an error response
            if (response.getType() == ResponseType.ERROR) {
                // Log the error but return an empty list instead of throwing an exception
                logger.warn("Error from server when retrieving tickets: {}", response.getError());
                return new ArrayList<>();
            }

            // Continue with normal processing if not an error
            Type listType = new TypeToken<List<TicketDTO>>(){}.getType();
            List<TicketDTO> ticketDTOs = gson.fromJson(gson.toJson(response.getData()), listType);

            List<Ticket> tickets = new ArrayList<>();
            for (TicketDTO dto : ticketDTOs) {
                Optional<Match> match = findOne(dto.getMatchId());
                Optional<User> ticketUser = Optional.of(user);
                Ticket ticket = DTOUtils.fromDTO(dto, match.orElse(null), ticketUser);
                tickets.add(ticket);
            }

            return tickets;
        } catch (Exception e) {
            logger.error("Exception in findTicketsBoughtByUser", e);
            return new ArrayList<>(); // Return empty list instead of throwing exception
        }
    }

    @Override
    public List<Ticket> findTicketsByCustomer(String name) throws ServicesException {
        try {
            Request request = new Request(RequestType.SEARCH_CUSTOMER_TICKETS, name);
            Response response = sendRequest(request);

            // Check if this is an error response
            if (response.getType() == ResponseType.ERROR) {
                // Log the error but return an empty list instead of throwing an exception
                logger.warn("Error from server when searching customer tickets: {}", response.getError());
                return new ArrayList<>();
            }

            Type listType = new TypeToken<List<TicketDTO>>(){}.getType();
            List<TicketDTO> ticketDTOs = gson.fromJson(gson.toJson(response.getData()), listType);

            List<Ticket> tickets = new ArrayList<>();
            for (TicketDTO dto : ticketDTOs) {
                try {
                    Optional<Match> match = findOne(dto.getMatchId());
                    Optional<User> ticketUser = Optional.empty();

                    if (dto.getUserId() != null) {
                        // Create a user object with the customer name
                        User namedUser = new User(dto.getUserId(), name, null);
                        ticketUser = Optional.of(namedUser);
                    }

                    Ticket ticket = DTOUtils.fromDTO(dto, match.orElse(null), ticketUser);
                    tickets.add(ticket);
                } catch (Exception e) {
                    // Log error for this ticket but continue processing others
                    logger.error("Error processing ticket {}: {}", dto.getId(), e.getMessage());
                }
            }

            return tickets;
        } catch (Exception e) {
            logger.error("Exception in findTicketsByCustomer: {}", e.getMessage(), e);
            return new ArrayList<>(); // Return empty list instead of throwing exception
        }
    }

    // TicketSellerServiceInterface implementations
    @Override
    public boolean isTicketSeller(String username) throws ServicesException {
        Request request = new Request(RequestType.CHECK_TICKET_SELLER, username);
        Response response = sendRequest(request);
        checkResponse(response);
        return gson.fromJson(gson.toJson(response.getData()), Boolean.class);
    }

    @Override
    public UserType authenticate(String username, String password) throws ServicesException {
        UserDTO userDTO = new UserDTO(null, username, password);
        Request request = new Request(RequestType.LOGIN, userDTO);
        Response response = sendRequest(request);

        if (response.getType() == ResponseType.USER) {
            // User is authenticated, now check if they are a ticket seller
            if (isTicketSeller(username)) {
                return UserType.SELLER;
            } else {
                return UserType.USER;
            }
        }
        // Authentication failed
        return UserType.UNKNOWN;
    }

    @Override
    public void registerObserver(User user, TicketObserver client) throws ServicesException {
        if (user == null || client == null) {
            throw new ServicesException("User and observer cannot be null");
        }

        Long userId = user.getId();
        observers.put(userId, client);
        logger.info("Observer registered for user: {}", userId);
    }

    @Override
    public void unregisterObserver(User user) throws ServicesException {
        if (user == null) {
            throw new ServicesException("User cannot be null");
        }

        Long userId = user.getId();
        TicketObserver removed = observers.remove(userId);

        if (removed != null) {
            logger.info("Observer unregistered for user: {}", userId);
        } else {
            logger.warn("No observer found to unregister for user: {}", userId);
        }
    }

    /**
     * Registers a client as an observer for ticket-related events
     */
    public void registerTicketObserver(User user, TicketObserver observer) throws ServicesException {
        if (user == null || observer == null) {
            throw new ServicesException("User and observer cannot be null");
        }

        // Check if already registered
        Long userId = user.getId();
        if (registeredObserverIds.contains(userId)) {
            logger.info("Observer already registered for user: {}", userId);
            return;
        }

        // Store locally and track
        registeredObservers.put(userId, observer);
        registeredObserverIds.add(userId);

        logger.info("Observer registered for user: {}", user.getId());

        // Send registration request to server
        UserDTO userDTO = DTOUtils.toDTO(user);
        Request request = new Request(RequestType.REGISTER_OBSERVER, userDTO);
        Response response = sendRequest(request);
        checkResponse(response);

        // Start the update listener if not already started
        startUpdateListener();
    }

    /**
     * Unregisters a client as an observer
     */
    public void unregisterTicketObserver(User user) throws ServicesException {
        if (user == null) {
            throw new ServicesException("User cannot be null");
        }

        // Remove the observer locally
        registeredObservers.remove(user.getId());
        logger.info("Observer unregistered for user: {}", user.getId());

        // Send unregistration request to server
        UserDTO userDTO = DTOUtils.toDTO(user);
        Request request = new Request(RequestType.UNREGISTER_OBSERVER, userDTO);
        Response response = sendRequest(request);
        checkResponse(response);
    }

    /**
     * Starts a background thread to listen for updates from the server
     */
    private synchronized void startUpdateListener() {
        if (!listenerStarted) {
            Thread listenerThread = new Thread(this::listenForUpdates);
            listenerThread.setDaemon(true);
            listenerThread.start();
            listenerStarted = true;
            logger.info("Update listener started");
        }
    }


    /**
     * Process a consolidated update that contains multiple entity changes
     */
    @SuppressWarnings("unchecked")
    private void processConsolidatedUpdate(Response update) {
        try {
            matchCache.clear();
            lastMatchCacheUpdate = System.currentTimeMillis();

            // Extract consolidated data
            Map<String, Object> data = (Map<String, Object>) update.getData();
            String operation = (String) data.getOrDefault("operation", "UNKNOWN");
            logger.info("Processing CONSOLIDATED_UPDATE for operation: {}", operation);

            // Extract individual components
            MatchDTO matchDTO = null;
            TicketDTO ticketDTO = null;
            UserDTO userDTO = null;

            if (data.containsKey("match")) {
                matchDTO = gson.fromJson(gson.toJson(data.get("match")), MatchDTO.class);
            }

            if (data.containsKey("ticket")) {
                ticketDTO = gson.fromJson(gson.toJson(data.get("ticket")), TicketDTO.class);
            }

            if (data.containsKey("user")) {
                userDTO = gson.fromJson(gson.toJson(data.get("user")), UserDTO.class);
            }

            // Process in correct order: match → user → ticket
            if (matchDTO != null) {
                Match match = DTOUtils.fromDTO(matchDTO);
                notifyMatchUpdated(match);
            }

            if (userDTO != null) {
                User user = DTOUtils.fromDTO(userDTO);
                notifyUserTicketsChanged(user);
            }

            if (ticketDTO != null && matchDTO != null) {
                Match match = DTOUtils.fromDTO(matchDTO);

                Optional<User> user = Optional.empty();
                if (ticketDTO.getUserId() != null) {
                    user = getUserById(ticketDTO.getUserId());
                }

                Ticket ticket = DTOUtils.fromDTO(ticketDTO, match, user);
                notifyTicketSold(ticket);
            }

        } catch (Exception e) {
            logger.error("Error processing consolidated update: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a match update
     */
    private void processMatchUpdate(Response update) {
        try {
            MatchDTO matchDTO = gson.fromJson(gson.toJson(update.getData()), MatchDTO.class);
            Match match = DTOUtils.fromDTO(matchDTO);
            logger.info("PROCESSING MATCH_UPDATED for match: {}", match.getId());

            // Notify observers
            notifyMatchUpdated(match);
        } catch (Exception e) {
            logger.error("Error processing match update: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a ticket sold update
     */
    private void processTicketSoldUpdate(Response update) {
        try {
            TicketDTO ticketDTO = gson.fromJson(gson.toJson(update.getData()), TicketDTO.class);
            logger.info("PROCESSING TICKET_SOLD for matchId: {}", ticketDTO.getMatchId());

            // Get match data efficiently - use local cache if possible
            Optional<Match> matchOpt = findOne(ticketDTO.getMatchId());
            if (matchOpt.isPresent()) {
                Match soldMatch = matchOpt.get();

                // Create ticket object
                Optional<User> user = ticketDTO.getUserId() != null ?
                        getUserById(ticketDTO.getUserId()) : Optional.empty();
                Ticket ticket = DTOUtils.fromDTO(ticketDTO, soldMatch, user);

                // Notify observers about ticket sale
                notifyTicketSold(ticket);
            }
        } catch (Exception e) {
            logger.error("Error processing ticket sold update: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a user tickets change update
     */
    private void processUserTicketsUpdate(Response update) {
        try {
            UserDTO userDTO = gson.fromJson(gson.toJson(update.getData()), UserDTO.class);
            User user = DTOUtils.fromDTO(userDTO);
            logger.info("PROCESSING USER_TICKETS_CHANGED for user: {}", user.getId());

            // Notify observers
            notifyUserTicketsChanged(user);
        } catch (Exception e) {
            logger.error("Error processing user tickets update: {}", e.getMessage(), e);
        }
    }

    /**
     * Notifies observers that a match has been updated
     */
    private void notifyMatchUpdated(Match match) {
        logger.debug("Notifying observers about match update: {}", match.getId());

        // Notify registered observers
        for (TicketObserver observer : registeredObservers.values()) {
            executorService.submit(() -> {
                try {
                    observer.matchUpdated(match);
                } catch (Exception e) {
                    logger.error("Error delivering match update: {}", e.getMessage());
                }
            });
        }

        // Also notify direct observers
        for (TicketObserver observer : directObservers) {
            executorService.submit(() -> {
                try {
                    observer.matchUpdated(match);
                } catch (Exception e) {
                    logger.error("Error delivering match update to direct observer: {}", e.getMessage());
                }
            });
        }
    }


    /**
     * Notifies observers that a user's tickets have changed
     */
    private void notifyUserTicketsChanged(User user) {
        logger.debug("Notifying observers of user tickets changed: {}", user.getId());

        // Notify registered observers
        for (Map.Entry<Long, TicketObserver> entry : registeredObservers.entrySet()) {
            executorService.submit(() -> {
                try {
                    entry.getValue().userTicketsChanged(user);
                } catch (Exception e) {
                    logger.error("Error notifying observer {}: {}", entry.getKey(), e.getMessage(), e);
                }
            });
        }

        // Also notify direct observers
        for (TicketObserver observer : directObservers) {
            executorService.submit(() -> {
                try {
                    observer.userTicketsChanged(user);
                } catch (Exception e) {
                    logger.error("Error notifying direct observer: {}", e.getMessage(), e);
                }
            });
        }
    }

    /**
     * Notifies observers that a ticket has been sold
     */
    private void notifyTicketSold(Ticket ticket) {
        logger.debug("BasketballServicesProxy: Notifying ALL observers about ticket sold: {}", ticket.getId());

        // Notify registered observers
        for (TicketObserver observer : registeredObservers.values()) {
            executorService.submit(() -> {
                try {
                    logger.debug("Sending ticket sold event to observer");
                    observer.ticketSold(ticket);
                } catch (Exception e) {
                    logger.error("Error delivering ticket sold update: {}", e.getMessage());
                }
            });
        }

        // Also notify direct observers
        for (TicketObserver observer : directObservers) {
            executorService.submit(() -> {
                try {
                    observer.ticketSold(ticket);
                } catch (Exception e) {
                    logger.error("Error delivering ticket sold update to direct observer: {}", e.getMessage());
                }
            });
        }
    }
}