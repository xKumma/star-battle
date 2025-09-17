package be.kdg.team4.startbattle;

public class Move {
    public  int x;
    public  int y;

    public Move(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }
}