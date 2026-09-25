package co.wethinkcode.trafficflow;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class IntersectionDataCleaner {

    private static final Set<String> PLACEHOLDERS = Set.of(
            "",
            "n/a",
            "tbd",
            "unknown",
            "-",
            "nan"
    );

    public List<Intersection> loadAndClean()
            throws IOException, CsvValidationException {

        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("intersections-legacy.csv");

        if (inputStream == null) {
            throw new IOException(
                    "Could not find intersections-legacy.csv"
            );
        }

        Map<String, Intersection> uniqueIntersections =
                new LinkedHashMap<>();

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(
                        inputStream,
                        StandardCharsets.UTF_8
                )
        )) {
            reader.readNext(); // Skip the header

            String[] row;

            while ((row = reader.readNext()) != null) {
                if (row.length < 4) {
                    System.err.println(
                            "Rejected incomplete CSV row"
                    );
                    continue;
                }

                String id = normalizeId(row[0]);

                if (id == null) {
                    System.err.println(
                            "Rejected row with missing intersection ID"
                    );
                    continue;
                }

                Intersection cleaned = new Intersection(
                        id,
                        normalizeDistrict(row[1]),
                        normalizeSignalType(row[2]),
                        normalizeBoolean(row[3])
                );

                uniqueIntersections.merge(
                        id,
                        cleaned,
                        this::mergeDuplicates
                );
            }
        }

        return new ArrayList<>(uniqueIntersections.values());
    }

    private Intersection mergeDuplicates(
            Intersection existing,
            Intersection duplicate
    ) {
        return new Intersection(
                existing.id(),
                chooseValue(
                        existing.district(),
                        duplicate.district()
                ),
                chooseValue(
                        existing.signalType(),
                        duplicate.signalType()
                ),
                chooseValue(
                        existing.active(),
                        duplicate.active()
                )
        );
    }

    private <T> T chooseValue(T first, T second) {
        return first != null ? first : second;
    }

    private String normalizeId(String value) {
        String cleaned = normalizeSpaces(value);

        if (isMissing(cleaned)) {
            return null;
        }

        return cleaned.toUpperCase(Locale.ROOT);
    }

    private String normalizeDistrict(String value) {
        String cleaned = normalizeSpaces(value);

        if (isMissing(cleaned)) {
            return null;
        }

        String[] words = cleaned
                .toLowerCase(Locale.ROOT)
                .split(" ");

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(" ");
            }

            result.append(
                    Character.toUpperCase(word.charAt(0))
            );

            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }

    private String normalizeSignalType(String value) {
        String cleaned = normalizeSpaces(value);

        if (isMissing(cleaned)) {
            return null;
        }

        return cleaned.toLowerCase(Locale.ROOT);
    }

    private Boolean normalizeBoolean(String value) {
        String cleaned = normalizeSpaces(value);

        if (isMissing(cleaned)) {
            return null;
        }

        return switch (cleaned.toLowerCase(Locale.ROOT)) {
            case "y", "yes", "1", "true" -> true;
            case "n", "no", "0", "false" -> false;
            default -> null;
        };
    }

    private String normalizeSpaces(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private boolean isMissing(String value) {
        return PLACEHOLDERS.contains(
                value.toLowerCase(Locale.ROOT)
        );
    }
}
