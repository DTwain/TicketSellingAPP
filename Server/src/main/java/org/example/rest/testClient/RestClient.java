package org.example.rest.testClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.domain.Match;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

public class RestClient {
    private static final String BASE_URL = "http://localhost:8080/api/matches";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // IMPORTANT: Configure to ignore unknown properties
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void main(String[] args) {
        try {
            testRestServices();
        } catch (Exception e) {
            System.err.println("Error testing REST services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testRestServices() throws IOException, InterruptedException {
        System.out.println("=== Testing Java REST Client ===");

        // Test 1: Get all matches
        System.out.println("\n1. Testing GET all matches:");
        getAllMatches();

        // Test 2: Create a new match
        System.out.println("\n2. Testing POST create match:");
        Long newMatchId = createMatch();

        // Test 3: Get match by ID
        System.out.println("\n3. Testing GET match by ID:");
        getMatchById(newMatchId);

        // Test 4: Update match
        System.out.println("\n4. Testing PUT update match:");
        updateMatch(newMatchId);

        // Test 5: Delete match
        System.out.println("\n5. Testing DELETE match:");
        deleteMatch(newMatchId);

        System.out.println("\n=== All tests completed ===");
    }

    private static void getAllMatches() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }

    private static Long createMatch() throws IOException, InterruptedException {
        Match newMatch = new Match(null, "Test Team A", "Test Team B", LocalDateTime.now().plusDays(7));
        String matchJson = objectMapper.writeValueAsString(newMatch);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(matchJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());

        if (response.statusCode() == 201) {
            Match createdMatch = objectMapper.readValue(response.body(), Match.class);
            return createdMatch.getId();
        }
        return null;
    }

    private static void getMatchById(Long id) throws IOException, InterruptedException {
        if (id == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }

    private static void updateMatch(Long id) throws IOException, InterruptedException {
        if (id == null) return;

        Match updateMatch = new Match(id, "Updated Team A", "Updated Team B", LocalDateTime.now().plusDays(10));
        String matchJson = objectMapper.writeValueAsString(updateMatch);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(matchJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }

    private static void deleteMatch(Long id) throws IOException, InterruptedException {
        if (id == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }
}