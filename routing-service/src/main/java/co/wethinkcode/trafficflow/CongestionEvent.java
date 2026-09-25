package co.wethinkcode.trafficflow;

public record CongestionEvent(
        int level,
        String changedAt
) {
}