package be.kdg.team4.startbattle;

public class Renderer {
    public static final int SCREEN_WIDTH = 80;

    public static final String SEPARATOR = "-".repeat(SCREEN_WIDTH);


    public static final char STAR = '*';
    public static final char POINT = ' ';

    public static final String TITLE_TEXT = """
              _________ __              __________         __    __  .__         \s
             /   _____//  |______ ______\\______   \\_____ _/  |__/  |_|  |   ____ \s
             \\_____  \\\\   __\\__  \\\\_  __ \\    |  _/\\__  \\\\   __\\   __\\  | _/ __ \\\s
             /        \\|  |  / __ \\|  | \\/    |   \\ / __ \\|  |  |  | |  |_\\  ___/\s
            /_______  /|__| (____  /__|  |______  /(____  /__|  |__| |____/\\___  7
                    \\/           \\/             \\/      \\/                     \\/
            """;

    public static final String HEADER_TEXT = """
             ___  ____   __    ____  ____    __   ____  ____  __    ____\s
            / __)(_  _) /__\\  (  _ \\(  _ \\  /__\\ (_  _)(_  _)(  )  ( ___)
            \\__ \\  )(  /(__)\\  )   / ) _ < /(__)\\  )(    )(   )(__  )__)\s
            (___/ (__)(__)(__)(_)\\_)(____/(__)(__)(__)  (__) (____)(____)
            """;

    public static final String WIN_SCREEN_TEXT = """
             __   __  _______  __   __    _     _  ___   __    _  __
            |  | |  ||       ||  | |  |  | | _ | ||   | |  |  | ||  |
            |  |_|  ||   _   ||  | |  |  | || || ||   | |   |_| ||  |
            |       ||  | |  ||  |_|  |  |       ||   | |       ||  |
            |_     _||  |_|  ||       |  |       ||   | |  _    ||__|
              |   |  |       ||       |  |   _   ||   | | | |   | __
              |___|  |_______||_______|  |__| |__||___| |_|  |__||__|
           """;

    private static final String RULES_TEXT = """
            STARBATTLE GAME RULES:
            
                1. GRID SETUP:
            The game is played on rectangular grid( 8x8, 10x10, 14x14).
            
                2. STARS PLACEMENTS:
            Stars are placed on the grids.
            A fixed number of stars must be placed in specific rows and columns.
            
                3. NO ADJACENCY:
            Stars cannot be adjacent to each other, not even diagonally. This means no two star can touch in any direction (Horizontally,\s
            Vertically or Diagonally)
            
                4. ROWS, COLUMNS AND REGIONS LIMITS:\s
            Each row, column, and regions must contain the required amount of stars.
                    \s
                5. OBJECTIVE:
            The goal is to correctly fill the grid with stars, ensuring the placement follows the above rules and the grid is fully filled.
            
            """;


    public static void showTitleScreen() {
        printSeparator();

        printBlock(centerTextBlock(TITLE_TEXT));
        System.out.println();

        System.out.println(centerString("CONTROLS: (P)LAY | (L)EADERBOARD | (R)ULES"));

        printSeparator();
    }

    public static void showRuleScreen() {
        System.out.println();
        printBlock(centerTextBlock(HEADER_TEXT));

        printBlock(centerTextBlock(RULES_TEXT));
        System.out.println();

        System.out.println(centerString("CONTROLS: GO (B)ACK"));
        printSeparator();
    }

    public static void showGameScreen(Board board) {
        printBlock(centerTextBlock(HEADER_TEXT));
        System.out.println();

        printBlock(centerTextBlock(board.toString()));

        System.out.println();
        System.out.println(centerString("CONTROLS:" + (DatabaseConnection.IsConnected() ? " (S)AVE | " : "" ) + "(Q)UIT | or place/remove star  (x:y)"));

        printSeparator();
    }

    public static void showLeaderboardScreen() {
        System.out.println();
        String[][] leaderboardData = DatabaseConnection.IsConnected() ? DatabaseConnection.getLeaderboardData() : null;
        if (leaderboardData != null) {
            printBlock(centerTextBlock(HEADER_TEXT));
            System.out.println();
            printBlock(centerTextBlock(DatabaseConnection.createPrintableTableSimple(leaderboardData)));
        }else {
            System.out.println("No leaderboard data found.");
        }


        System.out.println();

        System.out.println(centerString("CONTROLS: GO (B)ACK"));
        printSeparator();
    }

    public static void showWinScreen(Timer timer) {
        printSeparator();

        System.out.println();
        printBlock(centerTextBlock(WIN_SCREEN_TEXT));
        System.out.println();

        System.out.println(centerString("CONGRATULATIONS!"));
        System.out.println(centerString("YOUR TIME IS: " + timer.getFormattedTime()));
        System.out.println();

        printSeparator();
    }

    public static String centerString(String text, int length) {
        if (text == null || text.length() >= length) {
            return text; // if text is null or too long, return as is
        }

        int totalPadding = length - text.length(); // total spaces needed
        int paddingLeft = totalPadding / 2;        // spaces on the left
        int paddingRight = totalPadding - paddingLeft; // spaces on the right

        return " ".repeat(paddingLeft) + text + " ".repeat(paddingRight);
    }

    public static String centerString(String text) {
        return centerString(text,SCREEN_WIDTH);
    }

    public static String[] centerTextBlock(String text, int length) {
        String[] block = text.split("\n");

        for (int i = 0; i < block.length; i++) {
            block[i] = centerString(block[i], length);
        }

        return block;
    }

    public static String[] centerTextBlock(String text) {
        return centerTextBlock(text,SCREEN_WIDTH);
    }

    public static void printBlock(String[] block) {
        for (String line : block) {
            System.out.println(line);
        }
    }

    public static void printSeparator() {
        System.out.println(SEPARATOR);
    }

}
