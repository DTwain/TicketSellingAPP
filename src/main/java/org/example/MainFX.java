package org.example;

import org.example.domain.Match;
import org.example.domain.Ticket;
import org.example.domain.User;
import org.example.repository.MatchRepository;
import org.example.repository.TicketRepository;
import org.example.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        // Instantiate repositories
        MatchRepository matchRepo = new MatchRepository();
        UserRepository userRepo = new UserRepository();
        TicketRepository ticketRepo = new TicketRepository();

        // --------------------------
        // Test Match Repository
        // --------------------------
        System.out.println("----- Testing Match Repository -----");
        Match match = new Match(1L, "Team A", "Team B", LocalDateTime.now());
        Optional<Match> matchSaveResult = matchRepo.save(match);
        if (matchSaveResult.isPresent()) {
            System.out.println("Match not saved: " + matchSaveResult.get());
        } else {
            System.out.println("Match saved successfully.");
        }

        Optional<Match> retrievedMatch = matchRepo.findOne(1L);
        retrievedMatch.ifPresentOrElse(
                m -> System.out.println("Found Match: " + m.getTeamA() + " vs " + m.getTeamB()),
                () -> System.out.println("Match not found")
        );

        // --------------------------
        // Test User Repository
        // --------------------------
        System.out.println("\n----- Testing User Repository -----");
        User user = new User(1L, "john_doe", "password123");
        Optional<User> userSaveResult = userRepo.save(user);
        if (userSaveResult.isPresent()) {
            System.out.println("User not saved: " + userSaveResult.get());
        } else {
            System.out.println("User saved successfully.");
        }

        Optional<User> retrievedUser = userRepo.findOne(1L);
        retrievedUser.ifPresentOrElse(
                u -> System.out.println("Found User: " + u.getUsername()),
                () -> System.out.println("User not found")
        );

        // --------------------------
        // Test Ticket Repository
        // --------------------------
        System.out.println("\n----- Testing Ticket Repository -----");
        // Ticket requires a Match; we use the same match we created
        Ticket ticket = new Ticket(1L, match, 42, false, 99.99);
        Optional<Ticket> ticketSaveResult = ticketRepo.save(ticket);
        if (ticketSaveResult.isPresent()) {
            System.out.println("Ticket not saved: " + ticketSaveResult.get());
        } else {
            System.out.println("Ticket saved successfully.");
        }

        Optional<Ticket> retrievedTicket = ticketRepo.findOne(1L);
        retrievedTicket.ifPresentOrElse(
                t -> System.out.println("Found Ticket: Seat " + t.getSeatNumber() + " for Match ID " + t.getMatch().getId()),
                () -> System.out.println("Ticket not found")
        );

        // --------------------------
        // Test update operations
        // --------------------------
        System.out.println("\n----- Testing Update Operations -----");
        // Update match: change team names
        match.setTeamA("Updated Team A");
        match.setTeamB("Updated Team B");
        Optional<Match> updateMatchResult = matchRepo.update(match);
        if (updateMatchResult.isEmpty()) {
            System.out.println("Match updated successfully.");
        } else {
            System.out.println("Match update failed: " + updateMatchResult.get());
        }

        // Update user: change password
        user.setPassword("newPassword");
        Optional<User> updateUserResult = userRepo.update(user);
        if (updateUserResult.isEmpty()) {
            System.out.println("User updated successfully.");
        } else {
            System.out.println("User update failed: " + updateUserResult.get());
        }

        // Update ticket: mark as sold and change price
        ticket.setSold(true);
        ticket.setPrice(120.00);
        Optional<Ticket> updateTicketResult = ticketRepo.update(ticket);
        if (updateTicketResult.isEmpty()) {
            System.out.println("Ticket updated successfully.");
        } else {
            System.out.println("Ticket update failed: " + updateTicketResult.get());
        }

        // --------------------------
        // Test delete operations
        // --------------------------
        System.out.println("\n----- Testing Delete Operations -----");
        Optional<Ticket> deletedTicket = ticketRepo.delete(ticket.getId());
        if (deletedTicket.isPresent()) {
            System.out.println("Ticket deleted: " + deletedTicket.get());
        } else {
            System.out.println("Ticket not found to delete.");
        }

        Optional<User> deletedUser = userRepo.delete(user.getId());
        if (deletedUser.isPresent()) {
            System.out.println("User deleted: " + deletedUser.get());
        } else {
            System.out.println("User not found to delete.");
        }

        Optional<Match> deletedMatch = matchRepo.delete(match.getId());
        if (deletedMatch.isPresent()) {
            System.out.println("Match deleted: " + deletedMatch.get());
        } else {
            System.out.println("Match not found to delete.");
        }
    }
}
