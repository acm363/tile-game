package boardgame.player;

import boardgame.board.Resource;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class Player {

    private final String name;
    private final Map<Resource, Integer> resources = new EnumMap<>(Resource.class);
    private int gold;

    public Player(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String name() {
        return name;
    }

    public int gold() {
        return gold;
    }

    public void addGold(int amount) {
        requirePositive(amount);
        gold += amount;
    }

    public void spendGold(int amount) {
        requirePositive(amount);
        if (amount > gold) {
            throw new IllegalStateException(name + " cannot spend " + amount + " gold out of " + gold);
        }
        gold -= amount;
    }

    public int resource(Resource resource) {
        return resources.getOrDefault(resource, 0);
    }

    public Map<Resource, Integer> resources() {
        return Map.copyOf(resources);
    }

    public void addResource(Resource resource, int quantity) {
        requirePositive(quantity);
        resources.merge(resource, quantity, Integer::sum);
    }

    public void removeResource(Resource resource, int quantity) {
        requirePositive(quantity);
        int held = resource(resource);
        if (quantity > held) {
            throw new IllegalStateException(name + " cannot remove " + quantity + " " + resource + " out of " + held);
        }
        if (quantity == held) {
            resources.remove(resource);
        } else {
            resources.put(resource, held - quantity);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    private static void requirePositive(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }
}
