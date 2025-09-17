package be.kdg.team4.startbattle;

public class Timer {
    private long startTime;
    private long elapsedTime;

    public Timer() {}

    public Timer(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public void activate() {
        startTime = System.currentTimeMillis();
    }

    public void recordElapsedTime() {
        long endTime = System.currentTimeMillis();
        elapsedTime += endTime - startTime;
    }

    public String getFormattedTime() {
        long hours = (elapsedTime / 3600000) % 24;
        long minutes = (elapsedTime / 60000) % 60;
        long seconds = (elapsedTime / 1000) % 60;
//        long milliseconds = elapsedTime % 1000;

        return String.format(
                "%02d:%02d:%02d",
                hours, minutes, seconds);

    }

    @Override
    public String toString() {
        return ("Elapsed Time: " + getFormattedTime());
    }
}
