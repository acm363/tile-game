package boardgame.unit;

import boardgame.player.Player;

public final class Army extends Unit {

    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 5;
    public static final int MAX_LEVEL = 3;

    private final int size;
    private int level;

    public Army(Player owner, int size) {
        super(owner);
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException("Army size must be between " + MIN_SIZE + " and " + MAX_SIZE + ": " + size);
        }
        this.size = size;
    }

    public int size() {
        return size;
    }

    public int level() {
        return level;
    }

    public void promote() {
        if (level == MAX_LEVEL) {
            throw new IllegalStateException("An army cannot rise above level " + MAX_LEVEL);
        }
        level++;
    }
}
