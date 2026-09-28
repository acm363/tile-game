package boardgame.unit;

import boardgame.player.Player;

public final class Army extends Unit {

    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 5;

    private int size;

    public Army(Player owner, int size) {
        super(owner);
        setSize(size);
    }

    public int size() {
        return size;
    }

    public void setSize(int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException("Army size must be between " + MIN_SIZE + " and " + MAX_SIZE + ": " + size);
        }
        this.size = size;
    }
}
