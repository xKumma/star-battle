package be.kdg.team4.startbattle;

import java.sql.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class DatabaseConnection {
    private static Connection connection;
    private static boolean isConnected = false;



    private static final String URL = "jdbc:postgresql://10.134.177.12:5432/starbattle";
    private static final String USER = "game";
    private static final String PASSWORD = "starbattle";
    private static final String DDL_PATH = "src/be/kdg/team4/startbattle/DDL.sql";

    private static int currentPlayerID = -1;
    private static int currentScenarioID;
    private static int currentSessionID = -1;

    private static final int LEADERBOARD_ROWS = 5;
    private static final int LEADERBOARD_COLUMNS = 5;
    private static final int TABLE_ENTRY_MAX_LENGTH = 12;
    private static final int TABLE_CELL_LENGTH = 14;


    public static void setConnection() {
        try  {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            isConnected = true;
        } catch (SQLException e) {
            System.err.println("Failed to connect to database.");
        }
    }

    // get userId based on the username
    public static int getUserID(String username) {
        // prepare the query
        String selectQuery = "SELECT player_id FROM players WHERE username = ?";
        int playerId = -1; // default value if no player is found

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
            preparedStatement.setString(1, username);

            // execute the query
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    playerId = resultSet.getInt("player_id"); // retrieve playerId
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (playerId != -1) currentPlayerID = playerId;

        return playerId;
    }

    public static boolean addPlayer(String username, String password) {
        boolean result = false;
        String insertQuery = "INSERT INTO players (username, password) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            // set the values for the placeholders
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            // execute the insert statement
            result = preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        currentPlayerID = getUserID(username);

        return result;
    }

    public static GameSession loadGameSession(int size, int starRule) {

        int[][] regions = new int[size][size];
        int[][] stars = new int[size][size];
        long time = 0;
        boolean empty = true;
        int starCount = 0;


        String selectQuery1 =
                """
                SELECT session_id, coordinatey, coordinatex, hasstarplaced
                FROM boardstate_tiles
                WHERE session_id = (SELECT session_id
                                    FROM sessions s
                                        JOIN players p ON s.player_id = p.player_id
                                        JOIN scenarios sc ON s.scenario_id = sc.scenario_id
                                    WHERE p.player_id = ?
                                        AND sc.boardsize = ?
                                        AND sc.starRule = ?
                                        AND s.issolved = false);
                """;

        String selectQuery2 =
                """
                SELECT DISTINCT s.scenario_id, sct.coordinatey, sct.coordinatex, sct.region, (EXTRACT (EPOCH FROM s.time) * 1000) AS time
                FROM scenario_tiles sct
                    JOIN sessions s ON s.scenario_id = sct.scenario_id
                WHERE s.session_id = ?;
                """;

        try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery1)) {
                preparedStatement.setInt(1, currentPlayerID);
                preparedStatement.setInt(2, size);
                preparedStatement.setInt(3, starRule);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        currentSessionID = resultSet.getInt("session_id");
                        empty = false;
                        while (resultSet.next()) {
                            int hasStar;
                            if (resultSet.getBoolean("hasstarplaced")) {
                                hasStar = 1;
                                starCount++;
                            }else {
                                hasStar = 0;
                            }
                            stars[resultSet.getInt("coordinatey")][resultSet.getInt("coordinatex")] = hasStar;
                        }
                    }
                }
            }

            if (!empty) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery2)) {
                    preparedStatement.setInt(1, currentSessionID);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            currentScenarioID = resultSet.getInt("scenario_id");
                            time = resultSet.getLong("time");
                            regions[resultSet.getInt("coordinatey")][resultSet.getInt("coordinatex")] = resultSet.getInt("region");
                            while (resultSet.next()) {
                                regions[resultSet.getInt("coordinatey")][resultSet.getInt("coordinatex")] = resultSet.getInt("region");
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!empty) {
            Board b = new Board(stars, regions, starRule);
            b.setStarCount(starCount);
            return new GameSession(b, time);
        }else {
            return null;
        }
    }

    public static GameSession newGameSession(int size, int starRule) {
        int[][] regions = new int[size][size];
        ArrayList<Integer> scenario_idList = new ArrayList<>();
        Random rand = new Random();

        String selectQuery1 =
                """
                SELECT scenario_id FROM scenarios
                WHERE boardsize = ? AND starRule = ?;
                """;

        try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery1)) {
                preparedStatement.setInt(1, size);
                preparedStatement.setInt(2, starRule);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        scenario_idList.add(resultSet.getInt(1));
                    }
                }
            }

            currentScenarioID = scenario_idList.get(rand.nextInt(scenario_idList.size()));

            String selectQuery2 =
                    """
                    SELECT coordinatey, coordinatex, region
                    FROM scenario_tiles
                    WHERE scenario_id = ?;
                    """;


            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery2)) {
                preparedStatement.setInt(1, currentScenarioID);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        regions[resultSet.getInt("coordinatey")][resultSet.getInt("coordinatex")] = resultSet.getInt("region");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        Scenario template = new Scenario(size, regions, starRule);
        Board b = new Board(template);
        createSessionEntry("0:0:0", false);
        return new GameSession(b);
    }

    public static void saveGame(Board board, Timer timer) {
        updateSessionEntry(timer.getFormattedTime(), false);
        if (currentSessionID != -1) {
            int[][] starPlacement = board.getStarPlacement();

            String updateQuery1 =
                    """
                    DELETE FROM boardstate_tiles WHERE session_id = ?;
                    """;

            try {
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery1)) {
                    preparedStatement.setInt(1, currentSessionID);
                    preparedStatement.executeUpdate();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            String updateQuery2 =
                """
                DELETE FROM sessions s
                USING scenarios sc
                WHERE s.scenario_id = sc.scenario_id
                AND player_id = ?
                AND boardsize = ?
                AND starRule = ?
                AND session_id != ?
                AND issolved = false;
                """;

            try {
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery2)) {
                    preparedStatement.setInt(1, currentPlayerID);
                    preparedStatement.setInt(2, board.getTemplate().getSize());
                    preparedStatement.setInt(3, board.getTemplate().getStarRule());
                    preparedStatement.setInt(4, currentSessionID);
                    preparedStatement.executeUpdate();
                }



                String updateQuery3 =
                    """
                    INSERT INTO boardstate_tiles (session_id,
                    coordinatey, coordinatex,hasstarplaced)
                    VALUES (?, ?, ?, ?);
                    """;


                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery3)) {
                    for (int i = 0; i < starPlacement.length; i++) {
                        for (int j = 0; j < starPlacement[i].length; j++) {
                            preparedStatement.setInt(1, currentSessionID);
                            preparedStatement.setInt(2, i);
                            preparedStatement.setInt(3, j);
                            preparedStatement.setBoolean(4, starPlacement[i][j] == 1);
                            preparedStatement.executeUpdate();
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }else System.err.println("Failed to create session entry. Gamestate not saved");

    }

    public static void wonGame(Timer timer) {
        updateSessionEntry(timer.getFormattedTime(), true);

        String updateQuery =
                """
                DELETE FROM boardstate_tiles WHERE session_id = ?;
                """;

        try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                preparedStatement.setInt(1, currentSessionID);
                preparedStatement.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void createSessionEntry(String time, boolean isSolved){
        int session_id = -1;

        String updateQuery =
                """
                INSERT INTO sessions (player_id, scenario_id, time, issolved)
                VALUES (?, ?, ?::interval, ?);
                """;

        try{
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setInt(1, currentPlayerID);
                preparedStatement.setInt(2, currentScenarioID);
                preparedStatement.setString(3, time);
                preparedStatement.setBoolean(4, isSolved);
                preparedStatement.executeUpdate();
                try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    while (resultSet.next()) {
                        session_id = resultSet.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        currentSessionID = session_id;

    }


    public static void updateSessionEntry(String time, Boolean isSolved){
        String updateQuery =
                """
                UPDATE sessions
                SET time = ?::interval, issolved = ?
                WHERE session_id = ?;
                """;

        try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                preparedStatement.setString(1, time);
                preparedStatement.setBoolean(2, isSolved);
                preparedStatement.setInt(3, currentSessionID);
                preparedStatement.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static String[][] getLeaderboardData() {
        String[][] leaderboard = new String[LEADERBOARD_ROWS + 1][LEADERBOARD_COLUMNS];
        boolean empty = true;

        String selectQuery =
                """
                SELECT DISTINCT row_number() over (ORDER BY boardsize DESC, sc.starrule DESC, TO_CHAR(MIN(sessions.time), 'MI:SS:MS') ASC) as "Rank",
                                p.username AS Player,
                                TO_CHAR(MIN(sessions.time), 'HH24:MI:SS') AS "Best Time",
                                sc.boardsize AS "Board Size",
                                sc.starRule AS "Stars"
                FROM sessions
                         JOIN players p ON p.player_id = sessions.player_id
                         JOIN scenarios sc ON sc.scenario_id = sessions.scenario_id
                WHERE sessions.issolved = TRUE
                GROUP BY p.player_id, username, sc.boardsize, sc.starrule
                ORDER BY "Rank"
                FETCH FIRST ? ROWS ONLY;
                """;

        try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
                preparedStatement.setInt(1, LEADERBOARD_ROWS);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

                    for (int j = 0; j < LEADERBOARD_COLUMNS; j++) {
                        leaderboard[0][j] = resultSetMetaData.getColumnName(j + 1);
                    }

                    int i = 1;
                    while (resultSet.next() && i <= LEADERBOARD_ROWS) {
                        empty = false;
                        for (int j = 0; j < LEADERBOARD_COLUMNS; j++) {
                            leaderboard[i][j] = resultSet.getString(j + 1);
                        }
                        i++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!empty) {
            return leaderboard;
        } else {
            return null;
        }
    }

    public static boolean checkPassword(String username, String password){
        boolean loginSuccess = false;
        String foundPassword = "";

        String selectQuery =
                """
                SELECT password FROM players
                WHERE username = ?;
                """;

        try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
                preparedStatement.setString(1, username);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        foundPassword = resultSet.getString("password");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (foundPassword.equals(password)){
            loginSuccess = true;
        }

        return loginSuccess;
    }



    public static void setUp(){
        if (!isConnected) return;

        try {
            Statement statement = connection.createStatement();

            BufferedReader reader = new BufferedReader(new FileReader(DDL_PATH));

            StringBuilder sqlStatement = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }

                sqlStatement.append(line).append(" ");

                if (line.endsWith(";")) {
                    statement.execute(sqlStatement.toString());
                    sqlStatement.setLength(0);
                }
            }

            } catch (SQLException | IOException e) {
            System.err.println("Failed to execute the SQL script.");
        }
    }

    public static void uploadScenario(int[][] regions, int size, int starRule){

        int scenario_id = -1;

        String updateQuery1 =
                """
                INSERT INTO scenarios(boardsize, starrule)
                VALUES (?, ?);
                """;
        try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery1, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setInt(1, size);
                preparedStatement.setInt(2, starRule);
                scenario_id = preparedStatement.executeUpdate();
                try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    while (resultSet.next()) {
                        scenario_id = resultSet.getInt(1);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        String updateQuery2 =
                """
                INSERT INTO scenario_tiles (scenario_id, coordinatey, coordinatex,region)
                VALUES (?, ?, ?, ?);
                """;

        try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery2)) {
                for (int i = 0; i < regions.length; i++) {
                    for (int j = 0; j < regions[i].length; j++) {
                        preparedStatement.setInt(1, scenario_id);
                        preparedStatement.setInt(2, i);
                        preparedStatement.setInt(3, j);
                        preparedStatement.setInt(4, regions[i][j]);
                        preparedStatement.executeUpdate();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static String createPrintableTableSimple(String[][] tableData) {

        StringBuilder printableTable = new StringBuilder();
        int currentRow = 0;

        for (int i = 0; i < (tableData.length*2)+1; i++) {
            int currentCell = 0;
            int cellPositionY = i % 2;
            for (int j = 0; j < ((tableData[0].length*(TABLE_CELL_LENGTH+2))+1); j++) {
                int cellPositionX = j % (TABLE_CELL_LENGTH+2);
                if (cellPositionY == 0){
                    if (cellPositionX == 0){
                        printableTable.append("+");
                    }else {
                        printableTable.append("—");
                    }
                }else {
                    if (cellPositionX == 0) {
                        printableTable.append("|");
                    } else if (cellPositionX == 1) {
                        printableTable.append(" ");
                    } else {
                        String entry = tableData[currentRow][currentCell];
                        if (entry.length() > TABLE_ENTRY_MAX_LENGTH) {
                            entry = entry.substring(0, entry.length() - (entry.length() - TABLE_ENTRY_MAX_LENGTH + 3)) + "...";
                        }
                        printableTable.append(entry).append(" ".repeat(Math.max(0, TABLE_CELL_LENGTH - tableData[currentRow][currentCell].length())));
                        j += TABLE_CELL_LENGTH - 1;
                        currentCell++;
                    }
                }
            }
            printableTable.append("\n");
            if (cellPositionY == 0 && i != 0){
                currentRow++;
            }
        }
        return printableTable.toString();
    }

    public static boolean IsConnected() {
        return isConnected;
    }
}