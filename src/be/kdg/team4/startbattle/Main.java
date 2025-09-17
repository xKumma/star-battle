package be.kdg.team4.startbattle;

public class Main {
    public static void main(String[] args) {
        DatabaseConnection.setConnection();
        DatabaseConnection.setUp();

        boolean gameOn = false;
        do {
            Renderer.showTitleScreen();

            Output output = InputManager.titleScreen();
            switch (output) {
                case PLAY -> {
                    gameOn = true;
                    Play();
                }
                case LEADERBOARD -> Leaderboard();
                case RULES -> Rules();
            }
        } while (!gameOn);
    }

    private static void Leaderboard() {
        Renderer.showLeaderboardScreen();
        Output output;
        do {
            output = InputManager.simpleScreen();
            if (output == Output.CANCEL) return;
        } while (output == null);
    }

    private static void Rules() {
        Renderer.showRuleScreen();
        Output output;
        do {
            output = InputManager.simpleScreen();
            if (output == Output.CANCEL) return;
        } while (output == null);

    }

    private static void Play(){
        Output loggedIn;
        do {
            loggedIn = InputManager.login();
            if (loggedIn == Output.CANCEL) return;
        } while (loggedIn == null);

        Renderer.printSeparator();

        boolean active = true;
        while (active) {
            boolean hasLoadingFailed = false;
            GameSession session;
            do {
                hasLoadingFailed = false;
                Output.GameType gameType;
                do {
                    gameType = InputManager.chooseGame();
                } while (gameType == null);

                Renderer.printSeparator();

                switch (gameType) {
                    case NEW8_1:
                    case NEW10_2:
                    case NEW14_3:
                        session = DatabaseConnection.IsConnected() ?
                                DatabaseConnection.newGameSession(8, gameType.getStarRule()) :
                                new GameSession(new Board(ScenarioReader.getScenarioFromSize(8)));
                        break;
                    case LOAD8_1:
                    case LOAD10_2:
                    case LOAD14_3:
                        session = DatabaseConnection.loadGameSession(gameType.getSize(), gameType.getStarRule());
                        if (session == null){
                            System.out.println("No saves found with selected size.");
                            hasLoadingFailed = true;
                        }
                        break;
                    default:
                        session = DatabaseConnection.IsConnected() ?
                                DatabaseConnection.newGameSession(8, gameType.getStarRule()) :
                                new GameSession(new Board(ScenarioReader.getScenarioFromSize(8)));
                }
            }while (hasLoadingFailed);

            session.startGame();

            active = InputManager.playAgain();
        }
    }
}
