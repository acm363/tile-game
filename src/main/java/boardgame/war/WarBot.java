package boardgame.war;

import boardgame.board.Board;
import boardgame.board.Position;
import boardgame.engine.Action;
import boardgame.engine.Attack;
import boardgame.engine.Decider;
import boardgame.engine.Deploy;
import boardgame.engine.Pass;
import boardgame.player.Player;
import boardgame.unit.Army;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class WarBot implements Decider {

    private final WarRules rules;
    private final Board board;
    private final int rounds;
    private final Random random;
    private final Map<Player, Integer> turnsTaken = new HashMap<>();

    public WarBot(WarRules rules, Board board, int rounds, Random random) {
        this.rules = rules;
        this.board = board;
        this.rounds = rounds;
        this.random = random;
    }

    @Override
    public Action choose(Player player, List<Action> legalActions) {
        int turn = turnsTaken.merge(player, 1, Integer::sum);
        List<Attack> attacks = legalActions.stream().filter(Attack.class::isInstance).map(Attack.class::cast).toList();
        if (!attacks.isEmpty()) {
            return best(attacks, Comparator.comparingInt(attack -> WarRules.power(armyAt(attack.target()))));
        }
        List<Position> tiles = legalActions.stream()
                .filter(Deploy.class::isInstance).map(action -> ((Deploy) action).position()).distinct().toList();
        if (tiles.isEmpty()) {
            return new Pass();
        }
        int maxSize = Math.min(Army.MAX_SIZE, rules.soldiers(player));
        int planned = Math.min(maxSize, Math.ceilDiv(rules.soldiers(player), Math.max(1, rounds - turn + 1)));
        List<Placement> placements = tiles.stream().map(tile -> place(player, tile, planned, maxSize)).toList();
        Placement chosen = best(placements, Comparator.<Placement>comparingInt(placement -> placement.safe ? 1 : 0)
                .thenComparingInt(placement -> placement.threats)
                .thenComparingInt(placement -> WarRules.range(board.terrain(placement.tile)))
                .thenComparingInt(placement -> -placement.size));
        return new Deploy(chosen.tile, chosen.size);
    }

    private Placement place(Player player, Position tile, int planned, int maxSize) {
        for (int size = planned; size <= maxSize; size++) {
            if (isSafe(player, tile, size)) {
                return new Placement(tile, size, true, threats(player, tile, size));
            }
        }
        return new Placement(tile, planned, false, threats(player, tile, planned));
    }

    private boolean isSafe(Player player, Position tile, int size) {
        int defence = WarRules.defence(new Army(player, size), board.terrain(tile));
        return enemies(player).stream().noneMatch(enemy ->
                WarRules.reaches(enemy, tile, board) && WarRules.power(armyAt(enemy)) > defence);
    }

    private int threats(Player player, Position tile, int size) {
        int power = WarRules.power(new Army(player, size));
        return (int) enemies(player).stream().filter(enemy ->
                WarRules.reaches(tile, enemy, board)
                        && power > WarRules.defence(armyAt(enemy), board.terrain(enemy))).count();
    }

    private List<Position> enemies(Player player) {
        return board.positions().stream()
                .filter(position -> board.occupant(position).filter(unit -> unit.owner() != player).isPresent())
                .toList();
    }

    private Army armyAt(Position position) {
        return (Army) board.occupant(position).orElseThrow();
    }

    private <T> T best(List<T> candidates, Comparator<T> order) {
        T top = candidates.stream().max(order).orElseThrow();
        List<T> tied = candidates.stream().filter(candidate -> order.compare(candidate, top) == 0).toList();
        return tied.get(random.nextInt(tied.size()));
    }

    private record Placement(Position tile, int size, boolean safe, int threats) {
    }
}
