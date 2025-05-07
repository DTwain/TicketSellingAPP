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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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

    @Override
    public void run() {
        while (connected) {
            try {
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
            } catch (IOException e) {
                logger.error("Error handling client request: {}", e.getMessage());
                connected = false;
            }
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

    private Response handleRequest(Request request) {
        try {
            switch (request.getType()) {
                case LOGIN:
                    return handleLogin(request);
                case LOGOUT:
                    return handleLogout();
                case REGISTER:
                    return handleRegister(request);
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

    private Response handleSellTickets(Request request) throws ServicesException {
        TicketSaleDTO saleDTO = gson.fromJson(gson.toJson(request.getData()), TicketSaleDTO.class);
        TicketSale sale = DTOUtils.fromDTO(saleDTO);
        Double totalPrice = services.getTicketService().sellTickets(sale);
        return new Response(ResponseType.TICKET_SALE_RESULT, totalPrice);
    }

    private Response handleGetUserTickets(Request request) throws ServicesException {
        UserDTO userDTO = gson.fromJson(gson.toJson(request.getData()), UserDTO.class);
        User user = DTOUtils.fromDTO(userDTO);
        List<Ticket> tickets = services.getTicketService().findTicketsBoughtByUser(user);
        List<TicketDTO> ticketDTOs = DTOUtils.toTicketDTOList(tickets);
        return new Response(ResponseType.TICKETS, ticketDTOs);
    }

    private Response handleSearchCustomerTickets(Request request) throws ServicesException {
        String customerName = gson.fromJson(gson.toJson(request.getData()), String.class);
        List<Ticket> tickets = services.getTicketService().findTicketsByCustomer(customerName);
        List<TicketDTO> ticketDTOs = DTOUtils.toTicketDTOList(tickets);
        return new Response(ResponseType.TICKETS, ticketDTOs);
    }

    private Response handleCheckTicketSeller(Request request) throws ServicesException {
        String username = gson.fromJson(gson.toJson(request.getData()), String.class);
        boolean isTicketSeller = services.getTicketSellerService().isTicketSeller(username);
        return new Response(ResponseType.IS_TICKET_SELLER, isTicketSeller);
    }
}