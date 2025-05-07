package org.example.network.rpc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.*;
import org.example.network.dto.*;
import org.example.network.protocol.Request;
import org.example.network.protocol.RequestType;
import org.example.network.protocol.Response;
import org.example.network.protocol.ResponseType;
import org.example.service.ServicesException;
import org.example.service.interfaces.MatchServiceInterface;
import org.example.service.interfaces.TicketSellerServiceInterface;
import org.example.service.interfaces.TicketServiceInterface;
import org.example.service.interfaces.UserServiceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BasketballServicesProxy implements UserServiceInterface, MatchServiceInterface, TicketServiceInterface, TicketSellerServiceInterface {

    private final String host;
    private final int port;
    private Socket connection;
    private BufferedReader input;
    private PrintWriter output;
    private final Gson gson;
    private static final Logger logger = LogManager.getLogger(BasketballServicesProxy.class);

    public BasketballServicesProxy(String host, int port) {
        this.host = host;
        this.port = port;
        this.gson = new Gson();
    }

    private void ensureConnected() throws ServicesException {
        try {
            if (connection == null || connection.isClosed()) {
                connection = new Socket(host, port);
                input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                output = new PrintWriter(connection.getOutputStream(), true);
                logger.info("Connected to server at {}:{}", host, port);
            }
        } catch (IOException e) {
            logger.error("Cannot connect to server: {}", e.getMessage());
            throw new ServicesException("Cannot connect to server", e);
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
        Request request = new Request(RequestType.LOGIN, userDTO);
        Response response = sendRequest(request);
        if (response.getType() == ResponseType.USER) {
            UserDTO responseDTO = gson.fromJson(gson.toJson(response.getData()), UserDTO.class);
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
        Request request = new Request(RequestType.GET_MATCH, id);
        Response response = sendRequest(request);

        if (response.getType() == ResponseType.MATCH) {
            MatchDTO matchDTO = gson.fromJson(gson.toJson(response.getData()), MatchDTO.class);
            return Optional.of(DTOUtils.fromDTO(matchDTO));
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
        checkResponse(response);
        return gson.fromJson(gson.toJson(response.getData()), String.class);
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
        UserDTO userDTO = DTOUtils.toDTO(user);
        Request request = new Request(RequestType.GET_USER_TICKETS, userDTO);
        Response response = sendRequest(request);
        checkResponse(response);

        Type listType = new TypeToken<List<TicketDTO>>(){}.getType();
        List<TicketDTO> ticketDTOs = gson.fromJson(gson.toJson(response.getData()), listType);

        // This is a simplification - in a real app you would need to handle Match and User references properly
        List<Ticket> tickets = new ArrayList<>();
        for (TicketDTO dto : ticketDTOs) {
            // Get match details for this ticket
            Optional<Match> match = findOne(dto.getMatchId());
            Optional<User> ticketUser = Optional.empty();

            if (dto.getUserId() != null) {
                // Get user details if available
                // In this simple implementation we just use the current user
                ticketUser = Optional.of(user);
            }

            Ticket ticket = DTOUtils.fromDTO(dto, match.orElse(null), ticketUser);
            tickets.add(ticket);
        }

        return tickets;
    }

    @Override
    public List<Ticket> findTicketsByCustomer(String name) throws ServicesException {
        Request request = new Request(RequestType.SEARCH_CUSTOMER_TICKETS, name);
        Response response = sendRequest(request);
        checkResponse(response);

        Type listType = new TypeToken<List<TicketDTO>>(){}.getType();
        List<TicketDTO> ticketDTOs = gson.fromJson(gson.toJson(response.getData()), listType);

        // Similar to findTicketsBoughtByUser, but we need to look up each match and user
        List<Ticket> tickets = new ArrayList<>();
        for (TicketDTO dto : ticketDTOs) {
            Optional<Match> match = findOne(dto.getMatchId());
            Optional<User> ticketUser = Optional.empty();

            if (dto.getUserId() != null) {
                // Ideally you would look up the user by ID, but for simplicity we'll use the name
                User namedUser = new User(null, name, null);
                ticketUser = Optional.of(namedUser);
            }

            Ticket ticket = DTOUtils.fromDTO(dto, match.orElse(null), ticketUser);
            tickets.add(ticket);
        }

        return tickets;
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
}