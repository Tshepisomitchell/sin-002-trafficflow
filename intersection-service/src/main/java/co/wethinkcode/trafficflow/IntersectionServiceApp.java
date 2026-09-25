package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IntersectionServiceApp {

    private static final int PORT = 7021;
    private static final String INGESTION_URL =
            "http://localhost:7020/intersections";

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newHttpClient();

    private static List<Intersection> intersections =
            List.of();

    public static void main(String[] args) {
        try {
            refreshIntersections();
        } catch (Exception exception) {
            System.err.println(
                    "Could not load ingestion data: "
                            + exception.getMessage()
            );
        }

        Javalin app = Javalin.create().start(PORT);

        app.get("/health", context ->
                context.result("OK")
        );

        app.get("/intersections", context ->
                context.json(intersections)
        );

        app.get("/intersections/{id}", context -> {
            String requestedId = context.pathParam("id")
                    .trim()
                    .toUpperCase(Locale.ROOT);

            intersections.stream()
                    .filter(intersection ->
                            intersection.id().equals(requestedId)
                    )
                    .findFirst()
                    .ifPresentOrElse(
                            context::json,
                            () -> context.status(404).json(
                                    Map.of(
                                            "error",
                                            "Unknown intersection",
                                            "id",
                                            requestedId
                                    )
                            )
                    );
        });

        app.get("/districts/{district}/intersections", context -> {
            String district = context.pathParam("district");

            List<Intersection> matches = intersections.stream()
                    .filter(intersection ->
                            intersection.district() != null
                    )
                    .filter(intersection ->
                            intersection.district()
                                    .equalsIgnoreCase(district)
                    )
                    .toList();

            if (matches.isEmpty()) {
                context.status(404).json(
                        Map.of(
                                "error",
                                "Unknown district",
                                "district",
                                district
                        )
                );
                return;
            }

            context.json(matches);
        });

        app.post("/refresh", context -> {
            try {
                refreshIntersections();

                context.json(Map.of(
                        "message",
                        "Intersection data refreshed",
                        "count",
                        intersections.size()
                ));
            } catch (Exception exception) {
                context.status(503).json(
                        Map.of(
                                "error",
                                "Ingestion service unavailable",
                                "details",
                                exception.getMessage()
                        )
                );
            }
        });
    }

    private static void refreshIntersections() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(INGESTION_URL))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Ingestion returned HTTP "
                            + response.statusCode()
            );
        }

        Intersection[] records = MAPPER.readValue(
                response.body(),
                Intersection[].class
        );

        intersections = Arrays.asList(records);
    }

    public record Intersection(
            String id,
            String district,
            String signalType,
            Boolean active
    ) {
    }
}