package be.kdg.team4.startbattle;

public class GameSession {
    Board board;
    Timer timer;

    public GameSession(Board board) {
        this.board = board;
        this.timer = new Timer();
    }
    public GameSession(Board board, long time) {
        this.board = board;
        this.timer = new Timer(time);
    }

    public void startGame() {
        timer.activate();
        boolean gameOver = false;
        do {
            Renderer.showGameScreen(board);
            Output.GameAction action = InputManager.validateGameInput(board.getTemplate().getSize());

            switch (action) {
                case MOVE -> {
                    System.out.println(InputManager.cachedMove);
                    board.toggleTile(InputManager.cachedMove.y - 1, InputManager.cachedMove.x - 1);
                    if (board.checkSolutionRules()){
                        gameOver = true;
                        timer.recordElapsedTime();
                        if (DatabaseConnection.IsConnected()) DatabaseConnection.wonGame(timer);
                        Renderer.showWinScreen(timer);
                    }
                }
                case SAVE -> {
                    timer.recordElapsedTime();
                    DatabaseConnection.saveGame(board, timer);
                }
                case QUIT -> {
                    gameOver = true;
                }
            }
        } while (!gameOver);
    }
}
