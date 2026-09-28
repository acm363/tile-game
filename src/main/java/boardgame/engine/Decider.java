package boardgame.engine;

import boardgame.player.Player;

import java.util.List;

@FunctionalInterface
public interface Decider {

    Action choose(Player player, List<Action> legalActions);
}
