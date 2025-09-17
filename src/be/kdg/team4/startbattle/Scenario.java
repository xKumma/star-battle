package be.kdg.team4.startbattle;

import java.util.Observable;

public class Scenario {
    private int size;
    private int starRule;
    private final int maxStars;
    private int[][] regionMap;

    public int getSize() {
        return size;
    }

    public int[][] getRegionMap() {
        return regionMap;
    }

    public int getMaxStars() {return maxStars;}

    public int getStarRule() {return starRule;}

    public Scenario(int size, int[][] regionMap, int starRule) {
        this.size = size;
        this.regionMap = regionMap;
        this.starRule = starRule;
        this.maxStars = size*starRule;
    }
}
