package org.example.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.Match;
import org.example.service.ServicesException;
import org.example.service.interfaces.MatchServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/matches")
@CrossOrigin
public class MatchRestController {

    @Autowired
    private MatchServiceInterface matchService;

    private static final Logger logger = LogManager.getLogger(MatchRestController.class);

    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {
        try {
            logger.info("REST: Getting all matches");
            List<Match> matches = matchService.findAll();
            return ResponseEntity.ok(matches);
        } catch (ServicesException e) {
            logger.error("Error getting all matches: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(@PathVariable Long id) {
        try {
            logger.info("REST: Getting match by id: {}", id);
            Optional<Match> match = matchService.findOne(id);
            if (match.isPresent()) {
                return ResponseEntity.ok(match.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (ServicesException e) {
            logger.error("Error getting match by id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Match> createMatch(@RequestBody Match match) {
        try {
            logger.info("REST: Creating match: {} vs {}", match.getTeamA(), match.getTeamB());

            // Ensure ID is null for new entities
            match.setId(null);

            Match savedMatch = matchService.save(match);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedMatch);
        } catch (ServicesException e) {
            logger.error("Error creating match: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Match> updateMatch(@PathVariable Long id, @RequestBody Match match) {
        try {
            logger.info("REST: Updating match with id: {}", id);

            // Check if match exists
            Optional<Match> existingMatch = matchService.findOne(id);
            if (existingMatch.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Set the ID from path parameter
            match.setId(id);

            Match updatedMatch = matchService.update(match);
            return ResponseEntity.ok(updatedMatch);
        } catch (ServicesException e) {
            logger.error("Error updating match with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        try {
            logger.info("REST: Deleting match with id: {}", id);

            // Check if match exists
            Optional<Match> existingMatch = matchService.findOne(id);
            if (existingMatch.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            matchService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (ServicesException e) {
            logger.error("Error deleting match with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}