package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IngestionServiceApp {

    private static final int PORT = 7020;

    public static void main(String[] args) {
        IntersectionDataCleaner cleaner =
                new IntersectionDataCleaner();

        final List<Intersection> intersections;

        try {
            intersections = cleaner.loadAndClean();
        } catch (Exception exception) {
            System.err.println(
                    "Could not load intersection data: "
                            + exception.getMessage()
            );
            return;
        }

        Javalin app = Javalin.create().start(PORT);

        app.get("/health", context ->
                context.result("OK")
        );

        app.get("/intersections", context ->
                context.json(intersections)
        );

        app.get("/intersections/{id}", context -> {
            String requestedId = context
                    .pathParam("id")
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
                                            "Intersection not found",
                                            "id",
                                            requestedId
                                    )
                            )
                    );
        });

        app.get("/districts/{district}/intersections", context -> {
            String requestedDistrict = context
                    .pathParam("district")
                    .trim();

            List<Intersection> matches = intersections.stream()
                    .filter(intersection ->
                            intersection.district() != null
                    )
                    .filter(intersection ->
                            intersection.district()
                                    .equalsIgnoreCase(
                                            requestedDistrict
                                    )
                    )
                    .toList();

            if (matches.isEmpty()) {
                context.status(404).json(
                        Map.of(
                                "error",
                                "District not found",
                                "district",
                                requestedDistrict
                        )
                );
                return;
            }

            context.json(matches);
        });

        System.out.printf(
                "Ingestion service started on port %d with %d cleaned intersections%n",
                PORT,
                intersections.size()
        );
    }
}
