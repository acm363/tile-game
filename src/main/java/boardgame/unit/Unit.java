package boardgame.unit;

import boardgame.player.Player;

import java.util.Objects;

public abstract class Unit {

    private Player owner;
    private int gold;

    protected Unit(Player owner) {
        this.owner = Objects.requireNonNull(owner);
    }

    public Player owner() {
        return owner;
    }

    public void changeOwner(Player newOwner) {
        this.owner = Objects.requireNonNull(newOwner);
    }

    public int gold() {
        return gold;
    }

    public void addGold(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Gold amount must be positive: " + amount);
        }
        gold += amount;
    }
}
