package pozhidaev;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Phaser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Spider {

    private HttpClient client;
    private String baseUrl;
    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());
    private final Phaser phaser = new Phaser(1);
    private final ObjectMapper mapper = new ObjectMapper();

    public Spider(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<String> start() {
        submit("");
        phaser.arriveAndAwaitAdvance();
        return sortedMessages();
    }

    private void submit(String path) {
        phaser.register();
        Thread.ofVirtual().start(() -> {
            try {
                System.out.println("Submitting " + path);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/" + path))
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    handleResponse(resp.body());
                } else {
                    System.err.println("Non-200 for " + path + ": " + resp.statusCode());
                }
            } catch (Exception e) {
                System.err.println("Error fetching " + path + ": " + e.getMessage());
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }

    private void handleResponse(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            String msg = root.get("message").asText();
            messages.add(msg);

            JsonNode successors = root.withArray("successors");
            for (JsonNode node : successors) {
                submit(node.asText());
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
    }

    private List<String> sortedMessages() {
        List<String> sorted;
        synchronized (messages) {
            sorted = new ArrayList<>(messages);
        }
        Collections.sort(sorted);
        return sorted;
    }
}
