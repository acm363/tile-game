package boardgame.engine;

import boardgame.board.Board;
import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.player.Player;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public interface GameRules {

    int defaultRounds();

    void setUp(Player player);

    List<Action> legalActions(Player player, Board board);

    void apply(Action action, Player player, GameContext context);

    default boolean simultaneous() {
        return false;
    }

    default void resolve(Map<Player, Action> orders, GameContext context) {
        orders.forEach((player, action) -> apply(action, player, context));
    }

    void upkeep(Player player, GameContext context);

    int score(Player player, Board board);

    default List<GameEvent> preview(Action action, Player player, Board board) {
        return List.of();
    }

    default Map<String, Integer> reserves(Player player) {
        return Map.of();
    }

    default void harvest(Player player, GameContext context) {
        Map<Resource, Integer> harvested = new EnumMap<>(Resource.class);
        for (Position territory : context.board().territoriesOf(player)) {
            harvested.merge(context.board().terrain(territory).resource(), 1, Integer::sum);
        }
        harvested.forEach(player::addResource);
        if (!harvested.isEmpty()) {
            context.emit(new Harvested(player, Map.copyOf(harvested)));
        }
    }
}
