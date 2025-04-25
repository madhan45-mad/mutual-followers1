package com.example.mutualfollowers.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class StartupRunner implements CommandLineRunner {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void run(String... args) {
        String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook";

        Map<String, String> request = new HashMap<>();
        request.put("name", "John Doe");
        request.put("regNo ", "REG12347");
        request.put("email ", "john@example.com");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(generateUrl, request, Map.class);

            String webhook = (String) response.getBody().get("webhook ");
            String token = (String) response.getBody().get("accessToken ");
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            List<Map<String, Object>> users = (List<Map<String, Object>>) data.get("users");

            // Step 2: Mutual followers
            List<List<Integer>> outcome = findMutualFollowers(users);

            // Step 3: Prepare final result
            Map<String, Object> result = new HashMap<>();
            result.put("regNo ", "REG12347");
            result.put("outcome ", outcome);

            // Step 4: POST to webhook with retry
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(result, headers);

            for (int i = 0; i < 4; i++) {
                try {
                    restTemplate.postForEntity(webhook, entity, String.class);
                    System.out.println("✅ Posted to webhook successfully.");
                    break;
                } catch (Exception e) {
                    System.out.println("Retry " + (i + 1) + " failed: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("Error in API call: " + e.getMessage());
        }
    }

    private List<List<Integer>> findMutualFollowers(List<Map<String, Object>> users) {
        Map<Integer, Set<Integer>> followMap = new HashMap<>();
        List<List<Integer>> result = new ArrayList<>();

        for (Map<String, Object> user : users) {
            Integer id = (Integer) user.get("id");
            List<Integer> follows = (List<Integer>) user.get("follows ");
            followMap.put(id, new HashSet<>(follows));
        }

        for (Integer id : followMap.keySet()) {
            for (Integer other : followMap.get(id)) {
                if (followMap.containsKey(other) &&
                    followMap.get(other).contains(id) &&
                    id < other) {
                    result.add(Arrays.asList(id, other));
                }
            }
        }
        return result;
    }
}
