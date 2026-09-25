package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

public class RoutingServiceApp {

    private static final int PORT = 7023;

    private static final String INTERSECTION_URL =
            "http://localhost:7021/intersections/";

    private static final String CONGESTION_URL =
            "http://localhost:7022/congestion";

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newHttpClient();

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(PORT);

        app.get("/health", context ->
                context.result("OK")
        );

        app.get("/routes/{from}/{to}", context -> {
            String from = normalizeId(
                    context.pathParam("from")
            );

            String to = normalizeId(
                    context.pathParam("to")
            );

            try {
                if (!intersectionExists(from)) {
                    context.status(404).json(
                            Map.of(
                                    "error",
                                    "Unknown starting intersection",
                                    "intersectionId",
                                    from
                            )
                    );
                    return;
                }

                if (!intersectionExists(to)) {
                    context.status(404).json(
                            Map.of(
                                    "error",
                                    "Unknown destination intersection",
                                    "intersectionId",
                                    to
                            )
                    );
                    return;
                }

                int congestionLevel =
                        fetchCongestionLevel();

                int estimatedMinutes =
                        15 + congestionLevel * 5;

                context.json(
                        new RouteEstimate(
                                from,
                                to,
                                congestionLevel,
                                estimatedMinutes,
                                Instant.now().toString()
                        )
                );

            } catch (Exception exception) {
                context.status(503).json(
                        Map.of(
                                "error",
                                "A required service is unavailable",
                                "details",
                                exception.getMessage()
                        )
                );
            }
        });
    }

    private static boolean intersectionExists(String id)
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(INTERSECTION_URL + id))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 404) {
            return false;
        }

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Intersection service returned HTTP "
                            + response.statusCode()
            );
        }

        return true;
    }

    private static int fetchCongestionLevel()
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CONGESTION_URL))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Congestion service returned HTTP "
                            + response.statusCode()
            );
        }

        JsonNode json = MAPPER.readTree(response.body());
        return json.get("level").asInt();
    }

    private static String normalizeId(String id) {
        return id.trim().toUpperCase(Locale.ROOT);
    }

    public record RouteEstimate(
            String from,
            String to,
            int congestionLevel,
            int estimatedMinutes,
            String calculatedAt
    ) {
    }
}