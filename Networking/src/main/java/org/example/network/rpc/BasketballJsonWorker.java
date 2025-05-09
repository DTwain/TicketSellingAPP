package org.example.network.rpc;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.*;
import org.example.network.dto.*;
import org.example.network.protocol.Request;
import org.example.network.protocol.RequestType;
import org.example.network.protocol.Response;
import org.example.network.protocol.ResponseType;
import org.example.service.AllServices;
import org.example.service.ServicesException;
import org.example.utils.observer.TicketObserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BasketballJsonWorker implements Runnable {
    private final AllServices services;
    private final Socket client;
    private BufferedReader input;
    private PrintWriter output;
    private final Gson gson;
    private volatile boolean connected;
    private boolean isUpdateListener = false;
    private static final Logger logger = LogManager.getLogger(BasketballJsonWorker.class);

    public BasketballJsonWorker(AllServices services, Socket client) {
        this.services = services;
        this.client = client;
        this.gson = new Gson();
        this.connected = true;
        try {
            input = new BufferedReader(new InputStreamReader(client.getInputStream()));
            output = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException e) {
            logger.error("Error creating worker: {}", e.getMessage());
        }
    }

    // Override the run method to handle disconnection for update listeners
    @Override
    public void run() {
        try {
            while (connected) {
                // Skip normal request processing if this is an update listener
                if (isUpdateListener) {
                    // For update listeners, just keep the connection alive and wait for updates
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.warn("Update listener interrupted");
                        break;
                    }
                    continue;
                }

                // Regular request handling (existing code)
                String requestJson = input.readLine();
                if (requestJson == null) {
                    break; // Client disconnected
                }

                logger.debug("Received request: {}", requestJson);
                Request request = gson.fromJson(requestJson, Request.class);
                Response response = handleRequest(request);
                String responseJson = gson.toJson(response);
                logger.debug("Sending response: {}", responseJson);
                output.println(responseJson);
            }
        } catch (IOException e) {
            logger.error("Error handling client request: {}", e.getMessage());
        } finally {
            // Clean up
            if (isUpdateListener) {
                UpdateBroadcaster.getInstance().unregisterListener(this);
            }

            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (client != null) client.close();
                logger.info("Client connection closed");
            } catch (IOException e) {
                logger.error("Error closing connection: {}", e.getMessage());
            }
        }
    }

    private Response handleRequest(Request request) {
        try {
            switch (request.getType()) {
                case LOGIN:
                    return handleLogin(request);
                case LOGOUT:
                    return handleLogout();
                case REGISTER:
                    return handleRegister(request);
                case GET_USER_BY_USERNAME:
                    return handleGetUserByUsername(request);
                case GET_USER_BY_ID:
                    return handleGetUserByID(request);
                case GET_ALL_MATCHES:
                    return handleGetAllMatches();
                case GET_MATCH:
                    return handleGetMatch(request);
                case GET_AVAILABLE_TICKETS:
                    return handleGetAvailableTickets(request);
                case GET_PRICE_RANGE:
                    return handleGetPriceRange(request);
                case SELL_TICKETS:
                    return handleSellTickets(request);
                case GET_USER_TICKETS:
                    return handleGetUserTickets(request);
                case SEARCH_CUSTOMER_TICKETS:
                    return handleSearchCustomerTickets(request);
                case CHECK_TICKET_SELLER:
                    return handleCheckTicketSeller(request);
                case REGISTER_OBSERVER:
                    return handleRegisterObserver(request);
                case UNREGISTER_OBSERVER:
                    return handleUnregisterObserver(request);
                case START_UPDATE_LISTENER:
                    return handleStartUpdateListener();
                default:
                    return new Response(ResponseType.ERROR, "Unknown request type");
            }
        } catch (ServicesException e) {
            logger.error("Service exception: {}", e.getMessage());
            return new Response(ResponseType.ERROR, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            return new Response(ResponseType.ERROR, "Server error: " + e.getMessage());
        }
    }

    private Response handleLogin(Request request) throws ServicesException {
        UserDTO userDTO = gson.fromJson(gson.toJson(request.getData()), UserDTO.class);
        User user = DTOUtils.fromDTO(userDTO);

        // Use the UserType returned from authenticate
        UserType userType = services.getUserService().authenticate(user.getUsername(), user.getPassword());

        if (userType != UserType.UNKNOWN) {
            // Authentication successful
            Optional<User> loggedUser = services.getUserService().getUserByUsername(user.getUsername());
            if (loggedUser.isPresent()) {
                // Add user type information to the response
                UserDTO responseDTO = DTOUtils.toDTO(loggedUser.get());

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("user", responseDTO);
                responseData.put("userType", userType.toString());

                return new Response(ResponseType.USER, responseData);
            }
        }
        return new Response(ResponseType.ERROR, "Authentication failed");
    }

    private Response handleGetUserByUsername(Request request) throws ServicesException {
        UserDTO userDTO = gson.fromJson(gson.toJson(request.getData()), UserDTO.class);
        User user = DTOUtils.fromDTO(userDTO);

        Optional<User> loggedUser = services.getUserService().getUserByUsername(user.getUsername());
        if (loggedUser.isPresent()) {
            // Add user type information to the response
            UserDTO responseDTO = DTOUtils.toDTO(loggedUser.get());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", responseDTO);
            return new Response(ResponseType.USER, responseData);
        }
        return new Response(ResponseType.ERROR, "User with username: " + user.getUsername() + " was not found");
    }

    private Response handleGetUserByID(Request request) throws ServicesException {
        UserDTO userDTO = gson.fromJson(gson.toJson(request.getData()), UserDTO.class);
        User user = DTOUtils.fromDTO(userDTO);

        Optional<User> loggedUser = services.getUserService().getUserById(user.getId());
        if (loggedUser.isPresent()) {
            // Add user type information to the response
            UserDTO responseDTO = DTOUtils.toDTO(loggedUser.get());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", responseDTO);
            return new Response(ResponseType.USER, responseData);
        }
        return new Response(ResponseType.ERROR, "User with id: " + user.getId() + " was not found");
    }

    private Response handleLogout() {
        return new Response(ResponseType.OK, "Logged out successfully");
    }

    private Response handleRegister(Request request) throws ServicesException {
        UserDTO userDTO = gson.fromJson(gson.toJson(request.getData()), UserDTO.class);
        User user = DTOUtils.fromDTO(userDTO);
        boolean registered = services.getUserService().registerUser(user.getUsername(), user.getPassword());
        if (registered) {
            return new Response(ResponseType.OK, "Registered successfully");
        }
        return new Response(ResponseType.ERROR, "Registration failed");
    }

    private Response handleGetAllMatches() throws ServicesException {
        List<Match> matches = services.getMatchService().findAll();
        List<MatchDTO> matchDTOs = DTOUtils.toDTOList(matches);
        return new Response(ResponseType.MATCHES, matchDTOs);
    }

    private Response handleGetMatch(Request request) throws ServicesException {
        Long matchId = gson.fromJson(gson.toJson(request.getData()), Long.class);
        Optional<Match> match = services.getMatchService().findOne(matchId);
        if (match.isPresent()) {
            return new Response(ResponseType.MATCH, DTOUtils.toDTO(match.get()));
        }
        return new Response(ResponseType.ERROR, "Match not found");
    }

    private Response handleGetAvailableTickets(Request request) throws ServicesException {
        Long matchId = gson.fromJson(gson.toJson(request.getData()), Long.class);
        int count = services.getTicketService().countAvailableTicketsByMatch(matchId);
        return new Response(ResponseType.AVAILABLE_TICKETS, count);
    }

    private Response handleGetPriceRange(Request request) throws ServicesException {
        Long matchId = gson.fromJson(gson.toJson(request.getData()), Long.class);
        String priceRange = services.getTicketService().ticketPriceRangePerMatch(matchId);
        return new Response(ResponseType.PRICE_RANGE, priceRange);
    }

    /**
     * Handles a request to sell tickets to a customer.
     * Processes the sale and broadcasts a consolidated update to all connected clients.
     */
    private Response handleSellTickets(Request request) throws ServicesException {
        try {
            // Parse the ticket sale data from the request
            TicketSaleDTO saleDTO = gson.fromJson(gson.toJson(request.getData()), TicketSaleDTO.class);
            TicketSale sale = DTOUtils.fromDTO(saleDTO);

            logger.info("Processing ticket sale: matchId={}, customer={}, seats={}",
                    sale.getMatchId(), sale.getCustomerName(), sale.getSeatsPurchased());

            // Process the sale through the service layer
            Double totalPrice = services.getTicketService().sellTickets(sale);
            logger.info("Sale completed successfully: total price=${}", totalPrice);

            // Get the match data AFTER the sale to ensure we have the correct available tickets
            Optional<Match> matchOpt = services.getMatchService().findOne(sale.getMatchId());
            if (matchOpt.isEmpty()) {
                logger.error("Match not found after sale: {}", sale.getMatchId());
                return new Response(ResponseType.ERROR, "Match not found after sale");
            }

            // Get user info if applicable
            Optional<User> userOpt = services.getUserService().getUserByUsername(sale.getCustomerName());
            User user = userOpt.orElse(null);

            Match match = matchOpt.get();

            // CRITICAL: Get the current available tickets count and price range
            int availableTickets = services.getTicketService().countAvailableTicketsByMatch(match.getId());
            String priceRange = services.getTicketService().ticketPriceRangePerMatch(match.getId());

            // Update the match object with these values
            match.setAvailableTickets(availableTickets);
            match.setPriceRange(priceRange);

            logger.info("After sale: Match {} ({}): availableTickets={}, priceRange={}",
                    match.getId(), match.getMatchDescription(), availableTickets, priceRange);

            // Send individual updates instead of a consolidated update to avoid class casting issues
            try {
                // Send match update
                MatchDTO matchDTO = DTOUtils.toDTO(match);
                Response matchUpdate = new Response(ResponseType.MATCH_UPDATED, matchDTO);
                UpdateBroadcaster.getInstance().broadcastUpdate(matchUpdate);

                // Send user tickets update if applicable
                if (user != null) {
                    UserDTO userDTO = DTOUtils.toDTO(user);
                    Response userUpdate = new Response(ResponseType.USER_TICKETS_CHANGED, userDTO);
                    UpdateBroadcaster.getInstance().broadcastUpdate(userUpdate);
                }
            } catch (Exception e) {
                logger.error("Error broadcasting updates: {}", e.getMessage(), e);
                // Continue with the sale even if broadcasting fails
            }

            // Return the sale result to the client who performed the sale
            return new Response(ResponseType.TICKET_SALE_RESULT, totalPrice);
        } catch (ServicesException e) {
            logger.error("Service exception during ticket sale: {}", e.getMessage(), e);
            return new Response(ResponseType.ERROR, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error selling tickets: {}", e.getMessage(), e);
            return new Response(ResponseType.ERROR, "Failed to sell tickets: " + e.getMessage());
        }
    }

    private Response handleGetUserTickets(Request request) {
        try {
            UserDTO userDTO = gson.fromJson(gson.toJson(request.getData()), UserDTO.class);
            User user = DTOUtils.fromDTO(userDTO);

            // Log important details for debugging
            logger.debug("Finding tickets for user ID={}, username={}", user.getId(), user.getUsername());

            List<Ticket> tickets = services.getTicketService().findTicketsBoughtByUser(user);
            List<TicketDTO> ticketDTOs = DTOUtils.toTicketDTOList(tickets);
            return new Response(ResponseType.TICKETS, ticketDTOs);
        } catch (Exception e) {
            // Log the ACTUAL root cause with stack trace
            logger.error("Failed to retrieve tickets for user", e);
            // Don't concatenate duplicate messages
            return new Response(ResponseType.ERROR, e.getMessage());
        }
    }

    private Response handleSearchCustomerTickets(Request request) {
        try {
            String customerName = gson.fromJson(gson.toJson(request.getData()), String.class);

            // Log for debugging
            logger.debug("Searching tickets for customer name: {}", customerName);

            // Fetch tickets and convert to DTOs
            List<Ticket> tickets = services.getTicketService().findTicketsByCustomer(customerName);
            logger.debug("Found {} tickets for customer {}", tickets.size(), customerName);

            List<TicketDTO> ticketDTOs = DTOUtils.toTicketDTOList(tickets);
            return new Response(ResponseType.TICKETS, ticketDTOs);
        } catch (ServicesException e) {
            // Log the specific service exception
            logger.error("Service exception searching tickets for customer: {}", e.getMessage());
            return new Response(ResponseType.ERROR, e.getMessage());
        } catch (Exception e) {
            // Log unexpected exceptions with full stack trace
            logger.error("Unexpected error searching customer tickets", e);
            return new Response(ResponseType.ERROR, "Error searching tickets: " + e.getMessage());
        }
    }

    private Response handleCheckTicketSeller(Request request) throws ServicesException {
        String username = gson.fromJson(gson.toJson(request.getData()), String.class);
        boolean isTicketSeller = services.getTicketSellerService().isTicketSeller(username);
        return new Response(ResponseType.IS_TICKET_SELLER, isTicketSeller);
    }

    /**
     * Handles a request to register an observer for ticket events
     */
    private Response handleRegisterObserver(Request request) throws ServicesException {
        try {
            UserDTO userDTO = gson.fromJson(gson.toJson(request.getData()), UserDTO.class);
            User user = DTOUtils.fromDTO(userDTO);

            // Register the client connection in the server's observer registry
            services.getTicketService().registerObserver(user, new ClientObserver(user.getId()));

            logger.info("Registered observer for user: {}", user.getId());
            return new Response(ResponseType.OK, "Observer registered successfully");
        } catch (Exception e) {
            logger.error("Error registering observer: {}", e.getMessage());
            return new Response(ResponseType.ERROR, "Failed to register observer: " + e.getMessage());
        }
    }

    /**
     * Handles a request to unregister an observer
     */
    private Response handleUnregisterObserver(Request request) throws ServicesException {
        try {
            UserDTO userDTO = gson.fromJson(gson.toJson(request.getData()), UserDTO.class);
            User user = DTOUtils.fromDTO(userDTO);

            // Unregister the client from the server's observer registry
            services.getTicketService().unregisterObserver(user);

            logger.info("Unregistered observer for user: {}", user.getId());
            return new Response(ResponseType.OK, "Observer unregistered successfully");
        } catch (Exception e) {
            logger.error("Error unregistering observer: {}", e.getMessage());
            return new Response(ResponseType.ERROR, "Failed to unregister observer: " + e.getMessage());
        }
    }

    /**
     * Handles a request to start an update listener connection
     */
    private Response handleStartUpdateListener() {
        try {
            // Mark this connection as an update listener
            this.isUpdateListener = true;

            // Register this worker with the update broadcaster
            UpdateBroadcaster.getInstance().registerListener(this);

            logger.info("Started update listener for client: {}", client.getRemoteSocketAddress());
            return new Response(ResponseType.OK, "Update listener started successfully");
        } catch (Exception e) {
            logger.error("Error starting update listener: {}", e.getMessage());
            return new Response(ResponseType.ERROR, "Failed to start update listener: " + e.getMessage());
        }
    }

     /**
      *Sends an update to the client if this worker is an update listener
      */
     public void sendUpdate(Response update) {
         if (isUpdateListener && output != null) {
             try {
                 String updateJson = gson.toJson(update);
                 logger.debug("DIRECTLY sending update type {} to client", update.getType());

                 // CRITICAL: Make sure this actually sends the data
                 output.println(updateJson);
                 output.flush(); // Force flush the output stream to ensure delivery

                 logger.debug("Update sent: {}", updateJson);
             } catch (Exception e) {
                 logger.error("ERROR sending update to client: {}", e.getMessage(), e);
                 // Mark connection as invalid
                 this.connected = false;
             }
         } else {
             logger.warn("Cannot send update: isUpdateListener={}, output={}",
                     isUpdateListener, (output != null ? "available" : "null"));
         }
     }
    /**
     * Inner class representing an observer that forwards events to the client
     */
    private static class ClientObserver implements TicketObserver {

        public ClientObserver(Long userId) {
        }

        @Override
        public void matchUpdated(Match match) throws ServicesException {
            // Always broadcast match updates to all clients
            logger.debug("Server observer: Broadcasting match update to all clients");
            MatchDTO matchDTO = DTOUtils.toDTO(match);
            Response update = new Response(ResponseType.MATCH_UPDATED, matchDTO);
            UpdateBroadcaster.getInstance().broadcastUpdate(update);
        }

        @Override
        public void userTicketsChanged(User user) throws ServicesException {
            // Broadcast all user tickets changes to all clients - no filtering
            logger.debug("Server observer: Broadcasting user tickets changed for all clients");
            UserDTO userDTO = DTOUtils.toDTO(user);
            Response update = new Response(ResponseType.USER_TICKETS_CHANGED, userDTO);
            UpdateBroadcaster.getInstance().broadcastUpdate(update);
        }

        @Override
        public void ticketSold(Ticket ticket) throws ServicesException {
            // Broadcast all ticket sales to all clients
            logger.debug("Server observer: Broadcasting ticket sold to all clients");
            TicketDTO ticketDTO = DTOUtils.toDTO(ticket);
            Response update = new Response(ResponseType.TICKET_SOLD, ticketDTO);
            UpdateBroadcaster.getInstance().broadcastUpdate(update);
        }
    }


}
