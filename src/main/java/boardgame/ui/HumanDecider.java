package boardgame.ui;

import boardgame.engine.Action;
import boardgame.engine.Decider;
import boardgame.player.Player;

import java.util.List;
import java.util.Set;

public final class HumanDecider implements Decider {

    private final Set<Player> humans;
    private Action pending;

    public HumanDecider(Set<Player> humans) {
        this.humans = Set.copyOf(humans);
    }

    public boolean controls(Player player) {
        return humans.contains(player);
    }

    public void submit(Action action) {
        pending = action;
    }

    @Override
    public Action choose(Player player, List<Action> legalActions) {
        if (pending == null) {
            throw new IllegalStateException("No action was submitted for " + player);
        }
        Action action = pending;
        pending = null;
        return action;
    }
}
