package boardgame.engine;

import boardgame.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class RandomDecider implements Decider {

    private final Random random;

    public RandomDecider(Random random) {
        this.random = random;
    }

    @Override
    public Action choose(Player player, List<Action> legalActions) {
        Map<Class<?>, List<Action>> byKind = new LinkedHashMap<>();
        for (Action action : legalActions) {
            byKind.computeIfAbsent(action.getClass(), kind -> new ArrayList<>()).add(action);
        }
        List<List<Action>> kinds = List.copyOf(byKind.values());
        List<Action> chosenKind = kinds.get(random.nextInt(kinds.size()));
        return chosenKind.get(random.nextInt(chosenKind.size()));
    }
}
