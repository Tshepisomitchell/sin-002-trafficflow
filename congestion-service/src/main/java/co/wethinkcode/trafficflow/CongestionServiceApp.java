package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CongestionServiceApp {

    private static final int PORT = 7022;
    private static final AtomicInteger LEVEL =
            new AtomicInteger(0);

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(PORT);

        app.get("/health", context ->
                context.result("OK")
        );

        app.get("/congestion", context ->
                context.json(
                        new CongestionLevel(LEVEL.get())
                )
        );

        app.post("/congestion", context -> {
            CongestionLevel request;

            try {
                request = context.bodyAsClass(
                        CongestionLevel.class
                );
            } catch (Exception exception) {
                context.status(400).json(
                        Map.of(
                                "error",
                                "Request body must contain a numeric level"
                        )
                );
                return;
            }

            if (request.level() < 0 ||
                    request.level() > 8) {

                context.status(400).json(
                        Map.of(
                                "error",
                                "Congestion level must be between 0 and 8"
                        )
                );
                return;
            }

            LEVEL.set(request.level());
            context.json(request);
        });
    }

    public record CongestionLevel(int level) {
    }
}