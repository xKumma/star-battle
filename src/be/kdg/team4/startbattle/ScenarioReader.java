package be.kdg.team4.startbattle;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScenarioReader {
    private static final String SCENARIOS_PATH = "src/be/kdg/team4/startbattle/offline_scenarios.json";

    private static List<Scenario> scenarios = new ArrayList<>();

    public static Scenario getScenarioFromSize(int size) {
        if (scenarios.isEmpty()) try {
            readScenarioFile();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load offline scenarios from " + SCENARIOS_PATH + ": " + e.getMessage(), e);
        }

        List<Scenario> candidates = new ArrayList<>();
        for (Scenario scenario : scenarios) if (scenario.getSize() == size) candidates.add(scenario);
        if (candidates.isEmpty()) throw new IllegalArgumentException("No offline scenario with boardsize " + size);

        return candidates.get(new Random().nextInt(candidates.size()));
    }

    private static void readScenarioFile() throws Exception {
        String json = Files.readString(Path.of(SCENARIOS_PATH), StandardCharsets.UTF_8);
        parseJson(json);
        if (scenarios.isEmpty()) throw new IllegalStateException("No scenarios parsed from " + SCENARIOS_PATH);
    }

    private static void parseJson(String json) {
        String scenariosArray = extractArray(json, "scenarios");
        if (scenariosArray == null) return;

        Matcher obj = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL).matcher(scenariosArray);
        while (obj.find()) {
            String body = obj.group(1);

            Integer boardsize = extractInt(body, "boardsize");
            Integer starrule  = extractInt(body, "starrule");
            String regionsRaw = extractArray(body, "regions");

            if (boardsize == null || starrule == null || regionsRaw == null) continue;

            int[][] regions = parseRegions(regionsRaw);

            scenarios.add(new Scenario(boardsize, regions, starrule));
        }
    }

    private static Integer extractInt(String json, String key) {
        String pattern = "\"" + key + "\":\\s*(\\d+)";
        Matcher m = Pattern.compile(pattern).matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private static String extractArray(String json, String key) {
        int keyPos = json.indexOf("\"" + key + "\"");
        if (keyPos < 0) return null;
        int start = json.indexOf('[', keyPos);
        if (start < 0) return null;

        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) { escape = false; }
                else if (c == '\\') { escape = true; }
                else if (c == '"') { inString = false; }
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '[') { depth++; }
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(start + 1, i);
                }
            }
        }
        return null;
    }

    private static int[][] parseRegions(String innerArray) {
        String norm = innerArray.replaceAll("\\s+", "");
        if (norm.isEmpty()) return new int[0][0];

        if (norm.startsWith("[")) norm = norm.substring(1);
        if (norm.endsWith("]")) norm = norm.substring(0, norm.length() - 1);

        String[] rows = norm.split("\\],\\[");
        int[][] out = new int[rows.length][];
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r].replace("[", "").replace("]", "");
            if (row.isEmpty()) {
                out[r] = new int[0];
            } else {
                String[] nums = row.split(",");
                int[] vals = new int[nums.length];
                for (int i = 0; i < nums.length; i++) vals[i] = Integer.parseInt(nums[i]);
                out[r] = vals;
            }
        }
        return out;
    }
}
