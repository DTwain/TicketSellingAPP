package org.example.network.dto;

import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.TicketSale;
import org.example.domain.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DTOUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // User conversions
    public static UserDTO toDTO(User user) {
        if (user == null) return null;
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getPassword()
        );
    }

    public static User fromDTO(UserDTO dto) {
        if (dto == null) return null;
        return new User(
                dto.getId(),
                dto.getUsername(),
                dto.getPassword()
        );
    }


    // Match conversions
    public static MatchDTO toDTO(Match match) {
        if (match == null) return null;
        MatchDTO dto = new MatchDTO(
                match.getId(),
                match.getTeamA(),
                match.getTeamB(),
                match.getDateTime() != null ? match.getDateTime().format(FORMATTER) : null
        );
        dto.setAvailableTickets(match.getAvailableTickets());
        dto.setPriceRange(match.getPriceRange());
        return dto;
    }

    public static Match fromDTO(MatchDTO dto) {
        if (dto == null) return null;
        LocalDateTime dateTime = null;
        if (dto.getDateTime() != null && !dto.getDateTime().trim().isEmpty()) {
            try {
                dateTime = LocalDateTime.parse(dto.getDateTime(), FORMATTER);
            } catch (Exception e) {
                // Log error and leave dateTime as null
                System.err.println("Error parsing date: " + dto.getDateTime());
            }
        }

        Match match = new Match(
                dto.getId(),
                dto.getTeamA(),
                dto.getTeamB(),
                dateTime
        );
        match.setAvailableTickets(dto.getAvailableTickets());
        match.setPriceRange(dto.getPriceRange());
        return match;
    }

    // Ticket conversions
    public static TicketDTO toDTO(Ticket ticket) {
        if (ticket == null) return null;
        return new TicketDTO(
                ticket.getId(),
                ticket.getMatch() != null ? ticket.getMatch().getId() : null,
                ticket.getSeatNumber(),
                ticket.isSold(),
                ticket.getPrice(),
                ticket.getUser().isPresent() ? ticket.getUser().get().getId() : null
        );
    }

    public static Ticket fromDTO(TicketDTO dto, Match match, Optional<User> user) {
        if (dto == null) return null;
        return new Ticket(
                dto.getId(),
                match,
                dto.getSeatNumber(),
                dto.isSold(),
                dto.getPrice(),
                user
        );
    }

    // TicketSale conversions
    public static TicketSaleDTO toDTO(TicketSale ticketSale) {
        if (ticketSale == null) return null;
        return new TicketSaleDTO(
                ticketSale.getMatchId(),
                ticketSale.getCustomerName(),
                ticketSale.getCustomerAddress(),
                ticketSale.getSeatsPurchased()
        );
    }

    public static TicketSale fromDTO(TicketSaleDTO dto) {
        if (dto == null) return null;
        return new TicketSale(
                dto.getMatchId(),
                dto.getCustomerName(),
                dto.getCustomerAddress(),
                dto.getSeatsPurchased()
        );
    }

    // List conversions
    public static List<MatchDTO> toDTOList(List<Match> matches) {
        List<MatchDTO> dtos = new ArrayList<>();
        if (matches != null) {
            for (Match match : matches) {
                dtos.add(toDTO(match));
            }
        }
        return dtos;
    }

    public static List<Match> fromDTOList(List<MatchDTO> dtos) {
        List<Match> matches = new ArrayList<>();
        if (dtos != null) {
            for (MatchDTO dto : dtos) {
                matches.add(fromDTO(dto));
            }
        }
        return matches;
    }

    public static List<TicketDTO> toTicketDTOList(List<Ticket> tickets) {
        List<TicketDTO> dtos = new ArrayList<>();
        if (tickets != null) {
            for (Ticket ticket : tickets) {
                dtos.add(toDTO(ticket));
            }
        }
        return dtos;
    }
}