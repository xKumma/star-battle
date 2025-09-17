package be.kdg.team4.startbattle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;



public class Board {
    private final Scenario template;
    private int[][] starPlacement;
    private int starCount = 0;

    public int[][] getStarPlacement() {
        return starPlacement;
    }
    public Scenario getTemplate() {
        return template;
    }

    public void setStarCount(int starCount) {
        this.starCount = starCount;
    }

    public Board(int[][] starPlacement, int[][] regionMap, int starRule) {
        this(new Scenario(regionMap.length,regionMap,starRule));
        this.starPlacement = starPlacement.clone();
    }

    public Board(Scenario template) {
        this.template = template;
        // set up the board based on the template
        starPlacement = new int[template.getSize()][template.getSize()];
    }


    public void toggleTile(int y, int x) {
        if (starPlacement[y][x] != 1) {
            starPlacement[y][x] = 1;
            starCount++;
        }else {
            starPlacement[y][x] = 0;
            starCount--;
        }

    }

//    Since it's possible for a puzzle to have multiple solutions, I created this method
//    to check if the current boardstate is a valid solution. It goes over the board and
//    checks everything based on the rules. If it finds a mistake, then the solution is
//    not correct and the checking process ends.
    public boolean checkSolutionRules(){
        boolean errorFound = true;

        if (starCount == template.getMaxStars()) {
            errorFound = false;
            int starRule = template.getStarRule();
            int[][] regionMap = template.getRegionMap();
//        This collection is used to count how many stars have been found in each region.
            HashMap<Integer, Integer> starsFoundRegion = new HashMap<>();
            HashMap<Integer, Integer> starsFoundColumn = new HashMap<>();
//        For each row...
            for (int i = 0; i < template.getSize() && !errorFound; i++) {
                int starsFoundRow = 0;
//            in each square...
                for (int j = 0; j < template.getSize() && !errorFound; j++) {
//                if there is a star...
                    if (starPlacement[i][j] == 1) {
//                    ...make the necessary checks and increase the necessary counters for every rule.
//                    The for loop and switch exist only so that, after each step, it checks
//                    if a mistake has been found.
                        for (int k = 0; k < 5 && !errorFound; k++) {
                            switch (k) {
//                                Adjacency rule.
                                case 0 -> {
                                    for (int offsetY = -1; offsetY <= 1 && !errorFound; offsetY++) {
//                                        Makes sure it doesn't check out of bounds on y-axis.
                                        if ((i != 0 || offsetY != -1) && (i != template.getSize() - 1 || offsetY != 1)) {
//                                        Makes sure it doesn't check out of bounds on x-axis.
                                            for (int offsetX = -1; offsetX <= 1 && !errorFound; offsetX++) {
                                                if ((j != 0 || offsetX != -1) && (j != template.getSize() - 1 || offsetX != 1)) {
                                                    if (offsetY != 0 || offsetX != 0) {
                                                        if (starPlacement[i + offsetY][j + offsetX] == 1) {
                                                            errorFound = true;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Row rule
                                case 1 -> {
                                    starsFoundRow++;
                                    if (starsFoundRow > starRule) {
                                        errorFound = true;
                                    }
                                }
                                // Column rule
                                case 2 -> {
                                    starsFoundColumn.merge(j, 1 , Integer::sum);
                                    if (starsFoundColumn.get(j) > starRule) {
                                        errorFound = true;
                                    }
                                }
//                                if we reach the end of the column and the number of stars is too few, there is a mistake
                                case 3 -> {
                                    if (j == template.getSize() - 1 && starsFoundRow != starRule) {
                                        errorFound = true;
                                    }
                                }
//                                Region rule
                                case 4 -> {
                                    Integer region = regionMap[i][j];
                                    starsFoundRegion.merge(region, 1, Integer::sum);
                                    if (starsFoundRegion.get(region) > starRule) {
                                        errorFound = true;
                                    }
                                }
                            }
                        }

                    }
                }

            }
        }
        return !errorFound;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String line : getPrintLines()) {
            sb.append(line).append('\n');
        }
        
        return sb.toString();
    }

    private String[] getPrintLines() {
        List<String> lines = new ArrayList<>();

        for (int y = 0; y < template.getSize(); y++) {
            // for each tile we generate 2 lines, an upper border and a content line
            StringBuilder upperBorder = new StringBuilder();
            StringBuilder lowerBorder = new StringBuilder();
            StringBuilder contentLine = new StringBuilder();

            for (int x = 0; x < template.getSize(); x++) {
                // getting the neighbor code for each tile based on the tiles around it
                String neighborCode = CheckNeighbors(y, x, template.getRegionMap());

                // test neighbor codes
                //System.out.printf("NC[%d][%d] = %-3s %s\t", x, y, neighborCode, cornerSwitch(neighborCode));

                // setting the upper border
                upperBorder.append(String.format("%s%s", cornerSwitch(neighborCode), topSwitch(neighborCode)));
                contentLine.append(String.format("%s %c ", sideSwitch(neighborCode),
                        starPlacement[y][x] == 0 ? Renderer.POINT : starPlacement[y][x] == 1 ? Renderer.STAR : ' '));

                // add right border
                if (x == template.getSize() - 1) {
                    upperBorder.append(y == 0 ? "╗" : "║");
                    contentLine.append("║");
                }
            }

            lines.add(upperBorder.toString());
            lines.add(contentLine.toString());

            // add bottom border
            if (y == starPlacement.length - 1) {
                lowerBorder.append("╚");
                lowerBorder.append("=".repeat(Math.max(0, upperBorder.length() - 2)));
                lowerBorder.append("╝");
                lines.add(lowerBorder.toString());
            }

        }

        return lines.toArray(new String[0]);
    }

    private static String CheckNeighbors(int y, int x, int[][] regionMap) {
        StringBuilder output = new StringBuilder();
        //upper
        if (y != 0) {
            for (int offset = -1; offset < 1; offset++) {
                if (x+offset < 0) {
                    output.append(0);
                    continue;
                }
                output.append(regionMap[y][x] == regionMap[y - 1][x + offset] ? 1 : 0);
            }
        }else output.append("00");

        //inline
        if ((x != 0) && regionMap[y][x] == regionMap[y][x-1]) output.append(1);
        else output.append(0);

        return output.toString();
    }

    private static String cornerSwitch(String neighborCode) {
        return switch (neighborCode) {
            case "000" -> "╔";
            case "001" -> "=";
            case "010" -> "║";
            case "011" -> "╝";
            case "110" -> "╗";
            case "101" -> "╚";
            default -> "+";
        };
    }

    private static String topSwitch(String neighborCode) {
        return switch (neighborCode.charAt(1)) {
            case '1' -> " — ";
            case '0' -> "===";
            default -> "***";
        };
    }

    private static String sideSwitch(String neighborCode) {
        return switch (neighborCode.charAt(2)) {
            case '1' -> "|";
            case '0' -> "║";
            default -> "*";
        };
    }
}
