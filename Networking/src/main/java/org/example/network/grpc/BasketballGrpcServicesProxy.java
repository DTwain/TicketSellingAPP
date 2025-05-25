package org.example.network.grpc;

import com.basketball.ticket.grpc.*;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.*;
import org.example.service.ServicesException;
import org.example.service.interfaces.*;
import org.example.utils.observer.TicketObserver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.TicketSale;
import org.example.domain.User;
import org.example.domain.UserType;


/**
 * gRPC client proxy that implements all service interfaces
 * and provides connectivity to the C# gRPC server
 */
public class BasketballGrpcServicesProxy implements
        UserServiceInterface, MatchServiceInterface, TicketServiceInterface, TicketSellerServiceInterface {

    private static final Logger logger = LogManager.getLogger(BasketballGrpcServicesProxy.class);

    private final ManagedChannel channel;
    private final UserServiceGrpc.UserServiceBlockingStub userService;
    private final MatchServiceGrpc.MatchServiceBlockingStub matchService;
    private final TicketServiceGrpc.TicketServiceBlockingStub ticketService;
    private final TicketSellerServiceGrpc.TicketSellerServiceBlockingStub ticketSellerService;
    private final UpdateServiceGrpc.UpdateServiceStub updateService;

    // Observer management
    private final Map<Long, TicketObserver> registeredObservers = new ConcurrentHashMap<>();
    private final List<TicketObserver> directObservers = new ArrayList<>();
    private StreamObserver<UpdateEvent> updateStreamObserver;
    private boolean updateStreamActive = false;

    // Singleton instance
    private static BasketballGrpcServicesProxy instance;

    public BasketballGrpcServicesProxy(String host, int port) {
        // Create gRPC channel
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // For development - use TLS in production
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build();

        // Create service stubs
        this.userService = UserServiceGrpc.newBlockingStub(channel);
        this.matchService = MatchServiceGrpc.newBlockingStub(channel);
        this.ticketService = TicketServiceGrpc.newBlockingStub(channel);
        this.ticketSellerService = TicketSellerServiceGrpc.newBlockingStub(channel);
        this.updateService = UpdateServiceGrpc.newStub(channel);

        instance = this;
        logger.info("gRPC client connected to {}:{}", host, port);
    }

    public static BasketballGrpcServicesProxy getInstance() {
        return instance;
    }

    public void addObserverListener(TicketObserver observer) {
        if (observer != null) {
            synchronized (directObservers) {
                // Check if observer already exists before adding it
                if (!directObservers.contains(observer)) {
                    directObservers.add(observer);
                    logger.info("Added direct observer ({}), current count: {}",
                            observer.getClass().getSimpleName(), directObservers.size());
                } else {
                    logger.info("Observer ({}) already registered", observer.getClass().getSimpleName());
                }
            }
        } else {
            logger.warn("Attempted to add null observer");
        }
    }

    // UserServiceInterface implementation
    @Override
    public UserType authenticate(String username, String password) throws ServicesException {
        try {
            AuthenticateRequest request = AuthenticateRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .build();

            AuthenticateResponse response = userService.authenticate(request);
            return convertFromGrpcUserType(response.getUserType());
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error during authentication: {}", e.getStatus());
            throw new ServicesException("Authentication failed: " + e.getStatus().getDescription(), e);
        }
    }

    @Override
    public boolean usernameExists(String username) throws ServicesException {
        try {
            GetUserRequest request = GetUserRequest.newBuilder()
                    .setUsername(username)
                    .build();

            RegisterResponse response = userService.checkUsernameExists(request);
            return !response.getSuccess(); // Success means available, so invert
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error checking username: {}", e.getStatus());
            throw new ServicesException("Error checking username: " + e.getStatus().getDescription(), e);
        }
    }

    @Override
    public boolean registerUser(String username, String password) throws ServicesException {
        try {
            RegisterRequest request = RegisterRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .build();

            RegisterResponse response = userService.register(request);
            return response.getSuccess();
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error during registration: {}", e.getStatus());
            throw new ServicesException("Registration failed: " + e.getStatus().getDescription(), e);
        }
    }

    @Override
    public Optional<User> getUserByUsername(String username) throws ServicesException {
        try {
            GetUserRequest request = GetUserRequest.newBuilder()
                    .setUsername(username)
                    .build();

            GetUserResponse response = userService.getUserByUsername(request);
            if (response.hasUser()) {
                return Optional.of(convertFromGrpcUser(response.getUser()));
            }
            return Optional.empty();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            logger.error("gRPC error getting user by username: {}", e.getStatus());
            throw new ServicesException("Error getting user: " + e.getStatus().getDescription(), e);
        }
    }

    @Override
    public Optional<User> getUserById(Long id) throws ServicesException {
        try {
            GetUserRequest request = GetUserRequest.newBuilder()
                    .setUserId(id)
                    .build();

            GetUserResponse response = userService.getUserById(request);
            if (response.hasUser()) {
                return Optional.of(convertFromGrpcUser(response.getUser()));
            }
            return Optional.empty();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            logger.error("gRPC error getting user by ID: {}", e.getStatus());
            throw new ServicesException("Error getting user: " + e.getStatus().getDescription(), e);
        }
    }

    // MatchServiceInterface implementation
    @Override
    public List<Match> findAll() throws ServicesException {
        try {
            GetAllMatchesResponse response = matchService.getAllMatches(Empty.getDefaultInstance());
            List<Match> matches = new ArrayList<>();

            for (com.basketball.ticket.grpc.Match grpcMatch : response.getMatchesList()) {
                matches.add(convertFromGrpcMatch(grpcMatch));
            }

            return matches;
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error getting all matches: {}", e.getStatus());
            throw new ServicesException("Error getting matches: " + e.getStatus().getDescription(), e);
        }
    }

    @Override
    public Optional<Match> findOne(Long id) throws ServicesException {
        try {
            GetMatchRequest request = GetMatchRequest.newBuilder()
                    .setMatchId(id)
                    .build();

            GetMatchResponse response = matchService.getMatch(request);
            if (response.hasMatch()) {
                return Optional.of(convertFromGrpcMatch(response.getMatch()));
            }
            return Optional.empty();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            logger.error("gRPC error getting match: {}", e.getStatus());
            throw new ServicesException("Error getting match: " + e.getStatus().getDescription(), e);
        }
    }

    // TicketServiceInterface implementation
    @Override
    public int countAvailableTicketsByMatch(Long matchId) throws ServicesException {
        try {
            GetAvailableTicketsRequest request = GetAvailableTicketsRequest.newBuilder()
                    .setMatchId(matchId)
                    .build();

            GetAvailableTicketsResponse response = ticketService.getAvailableTickets(request);
            return response.getCount();
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error counting tickets: {}", e.getStatus());
            throw new ServicesException("Error counting tickets: " + e.getStatus().getDescription(), e);
        }
    }

    @Override
    public String ticketPriceRangePerMatch(Long matchId) throws ServicesException {
        try {
            GetPriceRangeRequest request = GetPriceRangeRequest.newBuilder()
                    .setMatchId(matchId)
                    .build();

            GetPriceRangeResponse response = ticketService.getPriceRange(request);
            return response.getPriceRange();
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error getting price range: {}", e.getStatus());
            throw new ServicesException("Error getting price range: " + e.getStatus().getDescription(), e);
        }
    }

    @Override
    public Double sellTickets(TicketSale ticketSale) throws ServicesException {
        try {
            com.basketball.ticket.grpc.TicketSale grpcTicketSale = convertToGrpcTicketSale(ticketSale);
            SellTicketsRequest request = SellTicketsRequest.newBuilder()
                    .setTicketSale(grpcTicketSale)
                    .build();

            SellTicketsResponse response = ticketService.sellTickets(request);
            return response.getTotalPrice();
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error selling tickets: {}", e.getStatus());
            throw new ServicesException("Error selling tickets: " + e.getStatus().getDescription(), e);
        }
    }

    @Override
    public List<Ticket> findTicketsBoughtByUser(User user) throws ServicesException {
        try {
            GetUserTicketsRequest request = GetUserTicketsRequest.newBuilder()
                    .setUserId(user.getId())
                    .build();

            GetUserTicketsResponse response = ticketService.getUserTickets(request);
            List<Ticket> tickets = new ArrayList<>();

            for (com.basketball.ticket.grpc.Ticket grpcTicket : response.getTicketsList()) {
                tickets.add(convertFromGrpcTicket(grpcTicket, user));
            }

            return tickets;
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error getting user tickets: {}", e.getStatus());
            return new ArrayList<>(); // Return empty list instead of throwing
        }
    }

    @Override
    public List<Ticket> findTicketsByCustomer(String name) throws ServicesException {
        try {
            SearchCustomerTicketsRequest request = SearchCustomerTicketsRequest.newBuilder()
                    .setCustomerName(name)
                    .build();

            SearchCustomerTicketsResponse response = ticketService.searchCustomerTickets(request);
            List<Ticket> tickets = new ArrayList<>();

            for (com.basketball.ticket.grpc.Ticket grpcTicket : response.getTicketsList()) {
                // Create a user object for the customer
                Optional<User> customerUser = Optional.of(new User(grpcTicket.getUserId(), name, null));
                tickets.add(convertFromGrpcTicket(grpcTicket, customerUser));
            }

            return tickets;
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error searching customer tickets: {}", e.getStatus());
            return new ArrayList<>(); // Return empty list instead of throwing
        }
    }

    // TicketSellerServiceInterface implementation
    @Override
    public boolean isTicketSeller(String username) throws ServicesException {
        try {
            CheckTicketSellerRequest request = CheckTicketSellerRequest.newBuilder()
                    .setUsername(username)
                    .build();

            CheckTicketSellerResponse response = ticketSellerService.checkTicketSeller(request);
            return response.getIsTicketSeller();
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error checking ticket seller: {}", e.getStatus());
            throw new ServicesException("Error checking ticket seller: " + e.getStatus().getDescription(), e);
        }
    }

    // Observer management methods
    @Override
    public void registerObserver(User user, TicketObserver client) throws ServicesException {
        registeredObservers.put(user.getId(), client);
        logger.info("Observer registered for user: {}", user.getId());
        startUpdateStream(user.getId());
    }

    @Override
    public void unregisterObserver(User user) throws ServicesException {
        registeredObservers.remove(user.getId());
        logger.info("Observer unregistered for user: {}", user.getId());
    }

    private void startUpdateStream(Long userId) {
        if (updateStreamActive) {
            return; // Stream already active
        }

        SubscribeUpdatesRequest request = SubscribeUpdatesRequest.newBuilder()
                .setUserId(userId)
                .build();

        updateStreamObserver = new StreamObserver<UpdateEvent>() {
            @Override
            public void onNext(UpdateEvent updateEvent) {
                try {
                    processUpdateEvent(updateEvent);
                } catch (Exception e) {
                    logger.error("Error processing update event", e);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Update stream error", t);
                updateStreamActive = false;
                // Implement reconnection logic here if needed
            }

            @Override
            public void onCompleted() {
                logger.info("Update stream completed");
                updateStreamActive = false;
            }
        };

        updateService.subscribeUpdates(request, updateStreamObserver);
        updateStreamActive = true;
        logger.info("Update stream started for user: {}", userId);
    }

    private void processUpdateEvent(UpdateEvent updateEvent) throws ServicesException {
        logger.info("Processing gRPC update event: {}", updateEvent.getEventCase());

        switch (updateEvent.getEventCase()) {
            case MATCH_UPDATED:
                MatchUpdatedEvent matchEvent = updateEvent.getMatchUpdated();
                Match match = convertFromGrpcMatch(matchEvent.getMatch());
                logger.info("Processing MATCH_UPDATED: matchId={}, availableTickets={}",
                        match.getId(), match.getAvailableTickets());
                notifyMatchUpdated(match);
                break;

            case TICKET_SOLD:
                TicketSoldEvent ticketEvent = updateEvent.getTicketSold();
                Ticket ticket = convertFromGrpcTicket(ticketEvent.getTicket(), Optional.empty());
                logger.info("Processing TICKET_SOLD: ticketId={}, matchId={}, userId={}",
                        ticket.getId(), ticket.getMatch() != null ? ticket.getMatch().getId() : "null",
                        ticket.getUser().isPresent() ? ticket.getUser().get().getId() : "null");
                notifyTicketSold(ticket);
                break;

            case USER_TICKETS_CHANGED:
                UserTicketsChangedEvent userEvent = updateEvent.getUserTicketsChanged();
                User user = convertFromGrpcUser(userEvent.getUser());
                logger.info("Processing USER_TICKETS_CHANGED: userId={}", user.getId());
                notifyUserTicketsChanged(user);
                break;

            default:
                logger.warn("Unknown update event type: {}", updateEvent.getEventCase());
                break;
        }
    }

    private void notifyMatchUpdated(Match match) {
        logger.info("Notifying {} registered observers + {} direct observers about match {} update",
                registeredObservers.size(), directObservers.size(), match.getId());

        for (TicketObserver observer : registeredObservers.values()) {
            try {
                observer.matchUpdated(match);
            } catch (Exception e) {
                logger.error("Error notifying observer about match update", e);
            }
        }

        for (TicketObserver observer : directObservers) {
            try {
                observer.matchUpdated(match);
            } catch (Exception e) {
                logger.error("Error notifying direct observer about match update", e);
            }
        }
    }

    private void notifyTicketSold(Ticket ticket) {
        logger.info("Notifying {} registered observers + {} direct observers about ticket {} sold",
                registeredObservers.size(), directObservers.size(), ticket.getId());

        for (TicketObserver observer : registeredObservers.values()) {
            try {
                observer.ticketSold(ticket);
            } catch (Exception e) {
                logger.error("Error notifying observer about ticket sold", e);
            }
        }

        for (TicketObserver observer : directObservers) {
            try {
                observer.ticketSold(ticket);
            } catch (Exception e) {
                logger.error("Error notifying direct observer about ticket sold", e);
            }
        }
    }

    private void notifyUserTicketsChanged(User user) {
        logger.info("Notifying {} registered observers + {} direct observers about user {} tickets change",
                registeredObservers.size(), directObservers.size(), user.getId());

        for (TicketObserver observer : registeredObservers.values()) {
            try {
                observer.userTicketsChanged(user);
            } catch (Exception e) {
                logger.error("Error notifying observer about user tickets change", e);
            }
        }

        for (TicketObserver observer : directObservers) {
            try {
                observer.userTicketsChanged(user);
            } catch (Exception e) {
                logger.error("Error notifying direct observer about user tickets change", e);
            }
        }
    }

    // Conversion helper methods
    private UserType convertFromGrpcUserType(com.basketball.ticket.grpc.UserType grpcUserType) {
        switch (grpcUserType) {
            case USER:
                return UserType.USER;
            case SELLER:
                return UserType.SELLER;
            default:
                return UserType.UNKNOWN;
        }
    }

    private User convertFromGrpcUser(com.basketball.ticket.grpc.User grpcUser) {
        return new User(grpcUser.getId(), grpcUser.getUsername(), grpcUser.getPassword());
    }

    private Match convertFromGrpcMatch(com.basketball.ticket.grpc.Match grpcMatch) {
        // Convert protobuf Timestamp to LocalDateTime
        Timestamp timestamp = grpcMatch.getDateTime();
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneOffset.UTC
        );

        Match match = new Match(grpcMatch.getId(), grpcMatch.getTeamA(), grpcMatch.getTeamB(), dateTime);
        match.setAvailableTickets(grpcMatch.getAvailableTickets());
        match.setPriceRange(grpcMatch.getPriceRange());
        return match;
    }

    private Ticket convertFromGrpcTicket(com.basketball.ticket.grpc.Ticket grpcTicket, User user) {
        Optional<User> userOpt = Optional.ofNullable(user);
        return convertFromGrpcTicket(grpcTicket, userOpt);
    }

    private Ticket convertFromGrpcTicket(com.basketball.ticket.grpc.Ticket grpcTicket, Optional<User> user) {
        // We need to get the match information
        Match match = null;
        try {
            Optional<Match> matchOpt = findOne(grpcTicket.getMatchId());
            match = matchOpt.orElse(null);
        } catch (ServicesException e) {
            logger.warn("Could not load match for ticket: {}", e.getMessage());
        }

        return new Ticket(
                grpcTicket.getId(),
                match,
                grpcTicket.getSeatNumber(),
                grpcTicket.getSold(),
                grpcTicket.getPrice(),
                user
        );
    }

    private com.basketball.ticket.grpc.TicketSale convertToGrpcTicketSale(TicketSale ticketSale) {
        return com.basketball.ticket.grpc.TicketSale.newBuilder()
                .setMatchId(ticketSale.getMatchId())
                .setCustomerName(ticketSale.getCustomerName())
                .setCustomerAddress(ticketSale.getCustomerAddress())
                .setSeatsPurchased(ticketSale.getSeatsPurchased())
                .build();
    }

    // Cleanup method
    public void shutdown() {
        try {
            if (updateStreamObserver != null) {
                updateStreamObserver.onCompleted();
            }

            channel.shutdown();
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
            logger.info("gRPC client shutdown completed");
        } catch (InterruptedException e) {
            logger.error("Error during shutdown", e);
            Thread.currentThread().interrupt();
        }
    }
}