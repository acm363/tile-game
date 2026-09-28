package boardgame.agriculture;

import boardgame.board.Board;
import boardgame.board.Position;
import boardgame.board.Resource;
import boardgame.board.Terrain;
import boardgame.engine.Action;
import boardgame.engine.Deploy;
import boardgame.engine.Exchange;
import boardgame.engine.GameContext;
import boardgame.engine.GameRules;
import boardgame.engine.Pass;
import boardgame.engine.Passed;
import boardgame.player.Player;
import boardgame.unit.Worker;

import java.util.ArrayList;
import java.util.List;

public final class AgricultureRules implements GameRules {

    static final int ROUNDS = 6;
    static final int INITIAL_GOLD = 15;

    @Override
    public int defaultRounds() {
        return ROUNDS;
    }

    @Override
    public void setUp(Player player) {
        player.addGold(INITIAL_GOLD);
    }

    @Override
    public List<Action> legalActions(Player player, Board board) {
        List<Action> actions = new ArrayList<>();
        actions.add(new Pass());
        board.freePositions().forEach(position -> actions.add(new Deploy(position, 1)));
        player.resources().forEach((resource, held) -> {
            for (int quantity = 1; quantity <= held; quantity++) {
                actions.add(new Exchange(resource, quantity));
            }
        });
        return actions;
    }

    @Override
    public void apply(Action action, Player player, GameContext context) {
        Board board = context.board();
        switch (action) {
            case Deploy deploy -> {
                board.place(deploy.position(), new Worker(player));
                context.emit(new WorkerDeployed(player, deploy.position(), board.terrain(deploy.position())));
            }
            case Exchange exchange -> {
                int gold = exchange.quantity() * price(exchange.resource());
                player.removeResource(exchange.resource(), exchange.quantity());
                player.addGold(gold);
                context.emit(new ResourceSold(player, exchange.resource(), exchange.quantity(), gold));
            }
            case Pass pass -> {
                int income = board.territoriesOf(player).stream()
                        .mapToInt(territory -> idleIncome(board.terrain(territory)))
                        .sum();
                player.addGold(income);
                context.emit(new Passed(player, income));
            }
            default -> throw new IllegalArgumentException("Unsupported action in the agriculture game: " + action);
        }
    }

    @Override
    public void upkeep(Player player, GameContext context) {
        Board board = context.board();
        int paid = 0;
        for (Position territory : board.territoriesOf(player)) {
            int wage = wage(board.terrain(territory));
            if (player.gold() >= wage) {
                player.spendGold(wage);
                board.occupant(territory).orElseThrow().addGold(wage);
                paid += wage;
            } else {
                board.remove(territory);
                context.emit(new WorkerDismissed(player, territory, wage));
            }
        }
        if (paid > 0) {
            context.emit(new WagesPaid(player, paid));
        }
    }

    @Override
    public int score(Player player, Board board) {
        return board.territoriesOf(player).stream()
                .mapToInt(territory -> board.occupant(territory).orElseThrow().gold())
                .sum();
    }

    static int price(Resource resource) {
        return switch (resource) {
            case ROCK -> 8;
            case SAND -> 5;
            case WOOD, WHEAT -> 2;
        };
    }

    static int wage(Terrain terrain) {
        return switch (terrain) {
            case MOUNTAIN -> 5;
            case DESERT -> 3;
            case FOREST, PLAIN -> 1;
            case OCEAN -> throw new IllegalArgumentException("No worker can stand on the ocean");
        };
    }

    static int idleIncome(Terrain terrain) {
        return switch (terrain) {
            case FOREST, PLAIN -> 1;
            case DESERT -> 2;
            case MOUNTAIN -> 0;
            case OCEAN -> throw new IllegalArgumentException("No worker can stand on the ocean");
        };
    }
}
