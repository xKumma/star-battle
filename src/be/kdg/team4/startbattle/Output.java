package be.kdg.team4.startbattle;

public enum Output {
    PLAY, LEADERBOARD, RULES,
    LOGGED_IN, CANCEL;

    public enum GameType {
        NEW8_1(8, 1), NEW10_2(10,2), NEW14_3(14,3),
        LOAD8_1(8,1), LOAD10_2(10,2), LOAD14_3(14,3);

        private final int size;
        private final int starRule;

        GameType(int size, int starRule) {
            this.size = size;
            this.starRule = starRule;
        }

        public int getSize() {
            return size;
        }

        public int getStarRule() {return starRule;}
    }

    public enum GameAction {
        MOVE, SAVE, QUIT;
    }
}
